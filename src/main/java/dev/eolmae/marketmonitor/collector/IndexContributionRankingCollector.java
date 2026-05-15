package dev.eolmae.marketmonitor.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.marketmonitor.common.enums.MarketType;
import dev.eolmae.marketmonitor.common.util.NumberParser;
import dev.eolmae.marketmonitor.domain.dashboard.IndexContributionRankingSnapshot;
import dev.eolmae.marketmonitor.domain.dashboard.repository.IndexContributionRankingSnapshotRepository;
import dev.eolmae.marketmonitor.domain.dashboard.MarketOverview;
import dev.eolmae.marketmonitor.domain.dashboard.repository.MarketOverviewRepository;
import dev.eolmae.marketmonitor.exception.EscalateException;
import dev.eolmae.marketmonitor.external.krx.crawler.KrxCrawler;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * KRX 전종목 시가총액 데이터 기반 지수기여도 상위 종목 수집기.
 *
 * 기여도 연산 공식:
 *   전일종가 = 현재가 - 전일대비등락액
 *   종목기여도 = (현재가 - 전일종가) × 상장주식수 / 전일_전체_시가총액 × 전일_지수값
 *
 * 전일 지수값: MarketOverview.indexValue - MarketOverview.changeValue 로 역산.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "stock.collection.enabled", havingValue = "true")
@RequiredArgsConstructor
public class IndexContributionRankingCollector {

    private static final String BLD = "dbms/MDC/STAT/standard/MDCSTAT01501";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int TOP_N = 50;

    private final KrxCrawler krxCrawler;
    private final IndexContributionRankingSnapshotRepository snapshotRepository;
    private final MarketOverviewRepository marketOverviewRepository;

    @Transactional
    public void collect(LocalDateTime snapshotTime) {
        for (MarketType marketType : MarketType.values()) {
            collectForMarket(marketType, snapshotTime);
        }
    }

    private void collectForMarket(MarketType marketType, LocalDateTime snapshotTime) {
        List<IndexContributionRankingSnapshot> existing =
            snapshotRepository.findBySnapshotTimeAndMarketTypeOrderByRankAsc(snapshotTime, marketType);
        if (!existing.isEmpty()) {
            log.debug("지수기여도랭킹 이미 존재, 스킵: market={}, snapshotTime={}", marketType, snapshotTime);
            return;
        }

        String mktId = Market.valueOf(marketType.name()).mktId;
        String trdDd = snapshotTime.toLocalDate().format(DATE_FMT);

        List<JsonNode> rows = fetchKrxRows(mktId, trdDd, marketType);

        BigDecimal prevTotalMarketCap = sumMarketCap(rows);
        if (prevTotalMarketCap.compareTo(BigDecimal.ZERO) == 0) {
            throw new EscalateException(
                "전일 전체 시가총액 합산 결과가 0: market=" + marketType + ", trdDd=" + trdDd
            );
        }

        BigDecimal prevIndexValue = resolvePrevIndexValue(marketType);

        List<ScoredStock> scored = new ArrayList<>();
        for (JsonNode row : rows) {
            String stockCode = row.path("ISU_SRT_CD").asText().trim();
            String stockName = row.path("ISU_ABBRV").asText().trim();
            if (stockCode.isBlank()) continue;

            BigDecimal curPrice = NumberParser.parseBigDecimal(row.path("TDD_CLSPRC").asText());
            BigDecimal priceChange = NumberParser.parseBigDecimal(row.path("CMPPREVDD_PRC").asText());
            BigDecimal listedShares = NumberParser.parseBigDecimal(row.path("LIST_SHRS").asText());

            BigDecimal prevPrice = curPrice.subtract(priceChange);
            BigDecimal contribution = calculateContribution(
                curPrice, prevPrice, listedShares, prevTotalMarketCap, prevIndexValue
            );

            // 등락률: priceChange / prevPrice * 100 (전일종가 기준)
            BigDecimal changeRate = BigDecimal.ZERO;
            if (prevPrice.compareTo(BigDecimal.ZERO) != 0) {
                changeRate = priceChange.divide(prevPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }

            scored.add(new ScoredStock(stockCode, stockName, contribution, changeRate));
        }

        // 기여도 절댓값 기준 상위 N건 (기여도 높은 순 = 상승 기여 → 하락 기여 순)
        scored.sort(Comparator.comparing(ScoredStock::contribution).reversed());

        int rank = 1;
        for (ScoredStock stock : scored.subList(0, Math.min(TOP_N, scored.size()))) {
            snapshotRepository.save(IndexContributionRankingSnapshot.create(
                marketType, rank++,
                stock.stockCode(), stock.stockName(),
                stock.contribution().setScale(4, RoundingMode.HALF_UP),
                stock.changeRate().setScale(4, RoundingMode.HALF_UP),
                snapshotTime
            ));
        }

        log.info("지수기여도랭킹 수집 완료: market={}, 저장건수={}", marketType, rank - 1);
    }

    private List<JsonNode> fetchKrxRows(String mktId, String trdDd, MarketType marketType) {
        Map<String, String> params = Map.of(
            "bld", BLD,
            "locale", "ko_KR",
            "mktId", mktId,
            "trdDd", trdDd,
            "share", "1",
            "money", "1",
            "csvxls_isNo", "false"
        );

        try {
            return krxCrawler.fetch(params);
        } catch (EscalateException e) {
            throw e;
        } catch (Exception e) {
            throw new EscalateException(
                "KRX 시가총액 데이터 수신 실패: market=" + marketType + ", trdDd=" + trdDd, e
            );
        }
    }

    /**
     * 응답 전종목의 시가총액(MKTCAP) 합산 → 전일 전체 시가총액으로 사용.
     */
    private BigDecimal sumMarketCap(List<JsonNode> rows) {
        return rows.stream()
            .map(node -> NumberParser.parseBigDecimal(node.path("MKTCAP").asText()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * MarketOverview에서 전일 지수값을 역산.
     * 전일 지수값 = 현재 지수(indexValue) - 등락액(changeValue)
     */
    private BigDecimal resolvePrevIndexValue(MarketType marketType) {
        MarketOverview overview = marketOverviewRepository.findByMarketType(marketType)
            .orElseThrow(() -> new EscalateException(
                "MarketOverview 데이터 없음 — 지수기여도 기여도 연산 불가: market=" + marketType
            ));
        return overview.getIndexValue().subtract(overview.getChangeValue());
    }

    /**
     * 종목 기여도 = (현재가 - 전일종가) × 상장주식수 / 전일_전체_시가총액 × 전일_지수값
     */
    private BigDecimal calculateContribution(
        BigDecimal curPrice, BigDecimal prevPrice,
        BigDecimal listedShares,
        BigDecimal prevTotalMarketCap,
        BigDecimal prevIndexValue
    ) {
        if (prevTotalMarketCap.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return curPrice.subtract(prevPrice)
            .multiply(listedShares)
            .divide(prevTotalMarketCap, MathContext.DECIMAL128)
            .multiply(prevIndexValue);
    }

	private enum Market {
		KOSPI("STK"),
		KOSDAQ("KSQ");
		final String mktId;  // KRX mktId
		Market(String mktId) { this.mktId = mktId; }
	}

    private record ScoredStock(
        String stockCode,
        String stockName,
        BigDecimal contribution,
        BigDecimal changeRate
    ) {}
}
