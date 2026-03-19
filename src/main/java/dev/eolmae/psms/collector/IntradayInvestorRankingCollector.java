package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.common.IntradayRankingType;
import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.dashboard.IntradayInvestorRankingSnapshot;
import dev.eolmae.psms.domain.dashboard.IntradayInvestorRankingSnapshotRepository;
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
public class IntradayInvestorRankingCollector {

	// plan.md 기준: ka10062 (KOSPI), ka10098 (KOSDAQ)
	// TODO: 키움 Open API 포털에서 경로 및 tr_id 확인 후 수정 필요
	private static final String API_PATH = "/api/dostk/stkinfo";
	private static final String TR_ID_KOSPI = "ka10062";
	private static final String TR_ID_KOSDAQ = "ka10098";

	private final KiwoomApiClient kiwoomApiClient;
	private final IntradayInvestorRankingSnapshotRepository repository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		for (MarketType marketType : MarketType.values()) {
			for (InvestorType investorType : InvestorType.values()) {
				for (IntradayRankingType rankingType : IntradayRankingType.values()) {
					try {
						collectForCombination(marketType, investorType, rankingType, snapshotTime);
					} catch (Exception e) {
						log.error("장중투자자랭킹 수집 실패: market={}, investor={}, ranking={}",
							marketType, investorType, rankingType, e);
					}
				}
			}
		}
	}

	private void collectForCombination(MarketType marketType, InvestorType investorType,
		IntradayRankingType rankingType, LocalDateTime snapshotTime) {

		List<IntradayInvestorRankingSnapshot> existing = repository
			.findBySnapshotTimeAndMarketTypeAndInvestorTypeAndRankingTypeOrderByRankAsc(
				snapshotTime, marketType, investorType, rankingType);
		if (!existing.isEmpty()) {
			log.debug("장중투자자랭킹 이미 존재, 스킵: market={}, investor={}, ranking={}, snapshotTime={}",
				marketType, investorType, rankingType, snapshotTime);
			return;
		}

		String trId = marketType == MarketType.KOSPI ? TR_ID_KOSPI : TR_ID_KOSDAQ;

		// TODO: 요청 파라미터 필드명 및 값 확인 필요
		String investorCode = toInvestorCode(investorType);
		String rankingCode = rankingType == IntradayRankingType.NET_BUY ? "1" : "2";  // TODO: 확인 필요

		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			trId,
			Map.of(
				"inv_type", investorCode,   // TODO: 파라미터명 확인 필요
				"sort_type", rankingCode    // TODO: 파라미터명 확인 필요
			)
		);

		// TODO: 응답 배열 필드명 확인 필요 (output 또는 output1)
		JsonNode outputList = response.path("output");
		int rank = 1;
		for (JsonNode item : outputList) {
			// TODO: 아래 필드명들은 키움 API 응답 문서 기준으로 수정 필요
			String stockCode = KiwoomResponseParser.parseString(item, "stck_shrn_iscd");
			String stockName = KiwoomResponseParser.parseString(item, "hts_kor_isnm");
			BigDecimal netBuyAmount = KiwoomResponseParser.parseBigDecimal(item, "ntby_tr_pbmn");
			long tradedVolume = KiwoomResponseParser.parseLong(item, "acml_vol");

			repository.save(IntradayInvestorRankingSnapshot.create(
				marketType, investorType, rankingType,
				rank++, stockCode, stockName, netBuyAmount, tradedVolume, snapshotTime
			));
		}

		log.debug("장중투자자랭킹 수집 완료: market={}, investor={}, ranking={}, count={}",
			marketType, investorType, rankingType, rank - 1);
	}

	// TODO: 키움 API의 실제 투자자 구분 코드 값 확인 후 수정 필요
	private String toInvestorCode(InvestorType investorType) {
		return switch (investorType) {
			case PERSONAL -> "01";      // TODO: 실제 코드 확인
			case FOREIGNER -> "04";     // TODO: 실제 코드 확인
			case INSTITUTION -> "10";   // TODO: 실제 코드 확인
		};
	}
}
