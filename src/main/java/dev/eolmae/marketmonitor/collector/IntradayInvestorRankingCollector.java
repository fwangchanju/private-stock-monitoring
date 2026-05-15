package dev.eolmae.marketmonitor.collector;

import dev.eolmae.marketmonitor.common.enums.AmtQtyType;
import dev.eolmae.marketmonitor.common.enums.IntradayRankingType;
import dev.eolmae.marketmonitor.common.enums.InvestorType;
import dev.eolmae.marketmonitor.common.enums.MarketType;
import dev.eolmae.marketmonitor.common.util.NumberParser;
import dev.eolmae.marketmonitor.domain.dashboard.IntradayInvestorRankingSnapshot;
import dev.eolmae.marketmonitor.domain.dashboard.repository.IntradayInvestorRankingSnapshotRepository;
import dev.eolmae.marketmonitor.external.kiwoom.client.KiwoomApiClient;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka10065Request;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka10065Response;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntradayInvestorRankingCollector {

	// ka10065: 장중투자자별매매상위요청 (순위정보 카테고리)

	private final KiwoomApiClient kiwoomApiClient;
	private final IntradayInvestorRankingSnapshotRepository repository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		for (MarketType marketType : MarketType.values()) {
			for (Investor investor : Investor.values()) {
				for (IntradayRankingType rankingType : IntradayRankingType.values()) {
					try {
						collectForCombination(marketType, investor, rankingType, snapshotTime);
					} catch (Exception e) {
						log.error("장중투자자랭킹 수집 실패: market={}, investor={}, ranking={}",
							marketType, investor, rankingType, e);
					}
				}
			}
		}
	}

	private void collectForCombination(MarketType marketType, Investor investor,
		IntradayRankingType rankingType, LocalDateTime snapshotTime) {

		List<IntradayInvestorRankingSnapshot> existing = repository
			.findBySnapshotTimeAndMarketTypeAndInvestorTypeAndRankingTypeOrderByRankAsc(
				snapshotTime, marketType, investor.domain, rankingType);
		if (!existing.isEmpty()) {
			log.debug("장중투자자랭킹 이미 존재, 스킵: market={}, investor={}, ranking={}, snapshotTime={}",
				marketType, investor, rankingType, snapshotTime);
			return;
		}

		var request = new Ka10065Request(rankingType.code(), Market.valueOf(marketType.name()).mrktTp, investor.code, AmtQtyType.AMOUNT.code());
		var response = kiwoomApiClient.post(request, Ka10065Response.class);

		if (response.items() == null) {
			return;
		}

		int rank = 1;
		for (Ka10065Response.RankingItem item : response.items()) {
			repository.save(IntradayInvestorRankingSnapshot.create(
				marketType, investor.domain, rankingType,
				rank++, item.stkCd(), item.stkNm(),
				NumberParser.parseBigDecimal(item.netslmt()),
				NumberParser.parseLong(item.buyQty()),
				snapshotTime
			));
		}

		log.debug("장중투자자랭킹 수집 완료: market={}, investor={}, ranking={}, count={}",
			marketType, investor, rankingType, rank - 1);
	}

	private enum Market {
		KOSPI("001"),
		KOSDAQ("101");
		final String mrktTp;  // ka10065 mrkt_tp
		Market(String mrktTp) { this.mrktTp = mrktTp; }
	}

	private enum Investor {
		FOREIGNER(InvestorType.FOREIGNER, "9000"),
		INSTITUTION(InvestorType.INSTITUTION, "9999"),
		FINANCIAL_INVESTMENT(InvestorType.FINANCIAL_INVESTMENT, "1000"),
		TRUST(InvestorType.TRUST, "3000"),
		PENSION_FUND(InvestorType.PENSION_FUND, "6000"),
		INSURANCE(InvestorType.INSURANCE, "2000"),
		BANK(InvestorType.BANK, "4000"),
		OTHER_FINANCE(InvestorType.OTHER_FINANCE, "5000"),
		GOVERNMENT(InvestorType.GOVERNMENT, "7000"),
		OTHER_CORP(InvestorType.OTHER_CORP, "7100"),
		FOREIGN_COMPANY(InvestorType.FOREIGN_COMPANY, "9100");

		final InvestorType domain;
		final String code;  // ka10065 trde_ori_tp
		Investor(InvestorType domain, String code) { this.domain = domain; this.code = code; }
	}
}
