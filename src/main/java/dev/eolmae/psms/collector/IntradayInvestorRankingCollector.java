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
import java.util.EnumMap;
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

	// ka10065: 장중투자자별매매상위요청 (순위정보 카테고리)
	// /api/dostk/rkinfo: 순위정보 카테고리 경로 (확정)
	private static final String API_PATH = "/api/dostk/rkinfo";
	private static final String TR_ID = "ka10065";

	// ka10065 orgn_tp 코드 매핑 (11종)
	private static final Map<InvestorType, String> ORGN_TP_CODE = new EnumMap<>(Map.ofEntries(
		Map.entry(InvestorType.FOREIGNER, "9000"),
		Map.entry(InvestorType.INSTITUTION, "9999"),
		Map.entry(InvestorType.FINANCIAL_INVESTMENT, "1000"),
		Map.entry(InvestorType.INSURANCE, "2000"),
		Map.entry(InvestorType.TRUST, "3000"),
		Map.entry(InvestorType.BANK, "4000"),
		Map.entry(InvestorType.OTHER_FINANCE, "5000"),
		Map.entry(InvestorType.PENSION_FUND, "6000"),
		Map.entry(InvestorType.GOVERNMENT, "7000"),
		Map.entry(InvestorType.OTHER_CORP, "7100"),
		Map.entry(InvestorType.FOREIGN_COMPANY, "9100")
	));

	private static final List<InvestorType> INVESTOR_TYPES = List.copyOf(ORGN_TP_CODE.keySet());

	private final KiwoomApiClient kiwoomApiClient;
	private final IntradayInvestorRankingSnapshotRepository repository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		for (MarketType marketType : MarketType.values()) {
			for (InvestorType investorType : INVESTOR_TYPES) {
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

		// mrkt_tp: 001=코스피, 101=코스닥
		String mrktTp = marketType == MarketType.KOSPI ? "001" : "101";
		// trde_tp: 1=순매수, 2=순매도
		String trdeTp = rankingType == IntradayRankingType.NET_BUY ? "1" : "2";
		String orgnTp = ORGN_TP_CODE.get(investorType);

		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			TR_ID,
			Map.of(
				"trde_tp", trdeTp,
				"mrkt_tp", mrktTp,
				"orgn_tp", orgnTp,
				"amt_qty_tp", "1"  // 1=금액
			)
		);

		// 응답 배열: opmr_invsr_trde_upper
		JsonNode outputList = response.path("opmr_invsr_trde_upper");
		int rank = 1;
		for (JsonNode item : outputList) {
			String stockCode = KiwoomResponseParser.parseString(item, "stk_cd");
			String stockName = KiwoomResponseParser.parseString(item, "stk_nm");
			BigDecimal netBuyAmount = KiwoomResponseParser.parseBigDecimal(item, "netslmt");
			long tradedVolume = KiwoomResponseParser.parseLong(item, "buy_qty");  // TODO: 거래량 전용 필드 확인

			repository.save(IntradayInvestorRankingSnapshot.create(
				marketType, investorType, rankingType,
				rank++, stockCode, stockName, netBuyAmount, tradedVolume, snapshotTime
			));
		}

		log.debug("장중투자자랭킹 수집 완료: market={}, investor={}, ranking={}, count={}",
			marketType, investorType, rankingType, rank - 1);
	}

}
