package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.dashboard.MarketOverview;
import dev.eolmae.psms.domain.dashboard.MarketOverviewRepository;
import dev.eolmae.psms.domain.dashboard.MarketOverviewSnapshot;
import dev.eolmae.psms.domain.dashboard.MarketOverviewSnapshotRepository;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.KiwoomResponseParser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketOverviewCollector {

	// ka20001: 업종현재가요청 (업종 카테고리)
	private static final String API_PATH = "/api/dostk/sect";
	private static final String TR_ID = "ka20001";

	private final KiwoomApiClient kiwoomApiClient;
	private final MarketOverviewRepository marketOverviewRepository;
	private final MarketOverviewSnapshotRepository marketOverviewSnapshotRepository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		for (MarketType marketType : MarketType.values()) {
			try {
				collectForMarket(marketType, snapshotTime);
			} catch (Exception e) {
				log.error("시장종합 수집 실패: market={}", marketType, e);
			}
		}
	}

	private void collectForMarket(MarketType marketType, LocalDateTime snapshotTime) {
		// mrkt_tp: 0=코스피, 1=코스닥
		// inds_cd: 001=코스피종합, 101=코스닥
		String mrktTp = marketType == MarketType.KOSPI ? "0" : "1";
		String indsCd = marketType == MarketType.KOSPI ? "001" : "101";

		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			TR_ID,
			Map.of(
				"mrkt_tp", mrktTp,
				"inds_cd", indsCd
			)
		);

		JsonNode output = response.path("output");
		LocalDateTime now = LocalDateTime.now();

		// 확인된 응답 필드명 (포털 문서 기준)
		BigDecimal indexValue = KiwoomResponseParser.parseBigDecimal(output, "cur_prc");
		BigDecimal changeValue = KiwoomResponseParser.parseBigDecimal(output, "pred_pre");
		BigDecimal changeRate = KiwoomResponseParser.parseBigDecimal(output, "flu_rt");
		BigDecimal tradingValue = KiwoomResponseParser.parseBigDecimal(output, "trde_prica");
		String marketStatus = KiwoomResponseParser.parseString(output, "mrkt_stat_cls_code");
		int advancers = KiwoomResponseParser.parseInt(output, "rising");
		int decliners = KiwoomResponseParser.parseInt(output, "fall");
		int unchangedCount = KiwoomResponseParser.parseInt(output, "stdns");

		MarketOverview overview = marketOverviewRepository.findByMarketType(marketType)
			.map(existing -> {
				existing.update(snapshotTime, now, marketStatus, indexValue, changeValue,
					changeRate, tradingValue, advancers, decliners, unchangedCount);
				return existing;
			})
			.orElseGet(() -> MarketOverview.create(marketType, snapshotTime, now, marketStatus,
				indexValue, changeValue, changeRate, tradingValue, advancers, decliners, unchangedCount));

		marketOverviewRepository.save(overview);

		if (marketOverviewSnapshotRepository.findByMarketTypeAndSnapshotTime(marketType, snapshotTime).isEmpty()) {
			marketOverviewSnapshotRepository.save(MarketOverviewSnapshot.from(overview));
		}

		log.debug("시장종합 수집 완료: market={}, index={}", marketType, indexValue);
	}
}
