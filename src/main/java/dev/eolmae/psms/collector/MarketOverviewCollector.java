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

	// TODO: 키움 Open API 포털(https://apiportal.kiwoom.com)에서 확인 후 수정 필요
	private static final String API_PATH = "/api/dostk/mrkcond";
	private static final String TR_ID = "FHKUP03500100";

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
		// TODO: 업종코드 값 확인 필요 (KOSPI: "0001", KOSDAQ: "1001" 일반적이지만 API 문서 기준으로 확인)
		String upjongCode = marketType == MarketType.KOSPI ? "0001" : "1001";

		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			TR_ID,
			Map.of("upjong_code", upjongCode)  // TODO: 요청 파라미터 필드명 확인 필요
		);

		// TODO: 응답 구조 확인 필요 (output 또는 output1)
		JsonNode output = response.path("output");
		LocalDateTime now = LocalDateTime.now();

		// TODO: 아래 필드명들은 키움 API 응답 문서 기준으로 수정 필요
		BigDecimal indexValue = KiwoomResponseParser.parseBigDecimal(output, "bstp_nmix_prpr");
		BigDecimal changeValue = KiwoomResponseParser.parseBigDecimal(output, "bstp_nmix_prdy_vrss");
		BigDecimal changeRate = KiwoomResponseParser.parseBigDecimal(output, "bstp_nmix_prdy_ctrt");
		BigDecimal tradingValue = KiwoomResponseParser.parseBigDecimal(output, "acml_tr_pbmn");
		String marketStatus = KiwoomResponseParser.parseString(output, "bstp_mrkt_hour_cls_code");
		int advancers = KiwoomResponseParser.parseInt(output, "bstp_rvsn_issu_cnt");
		int decliners = KiwoomResponseParser.parseInt(output, "bstp_fall_issu_cnt");
		int unchangedCount = KiwoomResponseParser.parseInt(output, "bstp_ntrt_issu_cnt");

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
