package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.history.ShortSellingHistory;
import dev.eolmae.psms.domain.history.ShortSellingHistoryRepository;
import dev.eolmae.psms.domain.stock.WatchStockRepository;
import dev.eolmae.psms.exception.EscalateException;
import dev.eolmae.psms.external.krx.KrxCrawler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * KRX 공매도 종합 현황 수집기.
 * 코스피(STK) + 코스닥(KSQ) 전종목 데이터를 수신 후, 관심종목만 ShortSellingHistory에 저장(upsert).
 * 하루 1회 수집이므로 실패 시 당일 데이터 영구 결손 → EscalateException 발생.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortSellingCollector {

    private static final String BLD = "dbms/MDC/STAT/srt/MDCSTAT30101";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KrxCrawler krxCrawler;
    private final ShortSellingHistoryRepository shortSellingHistoryRepository;
    private final WatchStockRepository watchStockRepository;

    @Transactional
    public void collect(LocalDate tradeDate) {
        Set<String> watchCodes = Set.copyOf(watchStockRepository.findDistinctStockCodes());
        if (watchCodes.isEmpty()) {
            log.info("관심종목 없음, 공매도 수집 스킵");
            return;
        }

        String trdDd = tradeDate.format(DATE_FMT);

        // 코스피 + 코스닥 전종목 수신 후 합산
        Map<String, KrxShortSellingRow> allRows = new HashMap<>();
        allRows.putAll(fetchMarket("STK", trdDd));
        allRows.putAll(fetchMarket("KSQ", trdDd));

        // 관심종목만 필터링하여 저장
        int savedCount = 0;
        for (String stockCode : watchCodes) {
            KrxShortSellingRow row = allRows.get(stockCode);
            if (row == null) {
                log.debug("관심종목 공매도 데이터 없음: stockCode={}", stockCode);
                continue;
            }
            upsert(stockCode, tradeDate, row);
            savedCount++;
        }

        log.info("공매도 수집 완료: tradeDate={}, 저장건수={}", tradeDate, savedCount);
    }

    private Map<String, KrxShortSellingRow> fetchMarket(String mktId, String trdDd) {
        Map<String, String> params = Map.of(
            "bld", BLD,
            "locale", "ko_KR",
            "searchType", "2",
            "mktId", mktId,
            "inqCond", "STMFRTSCIFDRFSSRSWBC",
            "trdDd", trdDd,
            "isuCd", "",
            "share", "1",
            "money", "1",
            "csvxls_isNo", "false"
        );

        List<JsonNode> rows;
        try {
            rows = krxCrawler.fetch(params);
        } catch (EscalateException e) {
            // KRX 구조 변경 등 복구 불가 장애는 그대로 전파
            throw e;
        } catch (Exception e) {
            throw new EscalateException(
                "KRX 공매도 데이터 수신 실패: mktId=" + mktId + ", trdDd=" + trdDd, e
            );
        }

        return rows.stream()
            .filter(node -> !node.path("ISU_CD").asText().isBlank())
            .collect(Collectors.toMap(
                node -> node.path("ISU_CD").asText().trim(),
                KrxShortSellingRow::from,
                // 중복 종목코드는 뒤 항목 우선 (실제 중복 없음)
                (a, b) -> b
            ));
    }

    private void upsert(String stockCode, LocalDate tradeDate, KrxShortSellingRow row) {
        var existing = shortSellingHistoryRepository.findByStockCodeAndTradeDate(stockCode, tradeDate);
        if (existing.isPresent()) {
            existing.get().update(
                row.shortVolume(), 0L,
                row.shortAmount(), BigDecimal.ZERO, row.shortRatio(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
        } else {
            shortSellingHistoryRepository.save(ShortSellingHistory.create(
                stockCode, tradeDate,
                row.shortVolume(), 0L,
                row.shortAmount(), BigDecimal.ZERO, row.shortRatio(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            ));
        }
    }

    /**
     * KRX 응답 OutBlock_1 한 행을 담는 내부 레코드.
     */
    private record KrxShortSellingRow(
        long shortVolume,
        BigDecimal shortAmount,
        BigDecimal shortRatio
    ) {
        static KrxShortSellingRow from(JsonNode node) {
            long shortVolume = parseVolume(node.path("CVSRTSELL_TRDVOL").asText());
            BigDecimal shortAmount = parseDecimal(node.path("CVSRTSELL_TRDVAL").asText());
            BigDecimal shortRatio = parseDecimal(node.path("TRDVOL_WT").asText());
            return new KrxShortSellingRow(shortVolume, shortAmount, shortRatio);
        }
    }

    private static long parseVolume(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) return 0L;
        try {
            return Long.parseLong(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
