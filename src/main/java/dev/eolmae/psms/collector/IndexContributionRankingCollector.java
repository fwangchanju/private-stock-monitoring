package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.dashboard.IndexContributionRankingSnapshot;
import dev.eolmae.psms.domain.dashboard.IndexContributionRankingSnapshotRepository;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.KiwoomResponseParser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexContributionRankingCollector {

	// TODO: 키움 Open API 포털에서 확인 후 수정 필요
	// 지수 기여도 상위 종목 조회 API
	private static final String API_PATH = "/api/dostk/indcontrib";
	private static final String TR_ID = "FHPUP03800100";  // TODO: 확인 필요

	private final KiwoomApiClient kiwoomApiClient;
	private final IndexContributionRankingSnapshotRepository repository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		for (MarketType marketType : MarketType.values()) {
			try {
				collectForMarket(marketType, snapshotTime);
			} catch (Exception e) {
				log.error("지수기여도랭킹 수집 실패: market={}", marketType, e);
			}
		}
	}

	private void collectForMarket(MarketType marketType, LocalDateTime snapshotTime) {
		List<IndexContributionRankingSnapshot> existing = repository
			.findBySnapshotTimeAndMarketTypeOrderByRankAsc(snapshotTime, marketType);
		if (!existing.isEmpty()) {
			log.debug("지수기여도랭킹 이미 존재, 스킵: market={}, snapshotTime={}", marketType, snapshotTime);
			return;
		}

		// TODO: 업종코드 파라미터 값 및 필드명 확인 필요
		String upjongCode = marketType == MarketType.KOSPI ? "0001" : "1001";

		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			TR_ID,
			Map.of("upjong_code", upjongCode)  // TODO: 파라미터명 확인 필요
		);

		// TODO: 응답 배열 필드명 확인 필요 (output 또는 output1)
		JsonNode outputList = response.path("output");
		int rank = 1;
		for (JsonNode item : outputList) {
			// TODO: 아래 필드명들은 키움 API 응답 문서 기준으로 수정 필요
			String stockCode = KiwoomResponseParser.parseString(item, "stck_shrn_iscd");
			String stockName = KiwoomResponseParser.parseString(item, "hts_kor_isnm");
			BigDecimal contributionScore = KiwoomResponseParser.parseBigDecimal(item, "cntr_bstp_nmix_prpr");
			BigDecimal priceChangeRate = KiwoomResponseParser.parseBigDecimal(item, "prdy_ctrt");

			repository.save(IndexContributionRankingSnapshot.create(
				marketType, rank++, stockCode, stockName, contributionScore, priceChangeRate, snapshotTime
			));
		}

		log.debug("지수기여도랭킹 수집 완료: market={}, count={}", marketType, rank - 1);
	}
}
