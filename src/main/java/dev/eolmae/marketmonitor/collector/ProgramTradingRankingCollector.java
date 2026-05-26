package dev.eolmae.marketmonitor.collector;

import dev.eolmae.marketmonitor.common.enums.AmtQtyType;
import dev.eolmae.marketmonitor.common.enums.MarketType;
import dev.eolmae.marketmonitor.common.enums.ProgramRankingType;
import dev.eolmae.marketmonitor.common.enums.StexType;
import dev.eolmae.marketmonitor.common.util.NumberParser;
import dev.eolmae.marketmonitor.domain.dashboard.ProgramTradingRankingSnapshot;
import dev.eolmae.marketmonitor.domain.dashboard.repository.ProgramTradingRankingSnapshotRepository;
import dev.eolmae.marketmonitor.external.kiwoom.client.KiwoomApiClient;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka90003Request;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka90003Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// ka90003: 프로그램순매수상위50요청 — 코스피/코스닥 × 순매수/순매도 × 금액/수량 = 8회 호출/사이클
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramTradingRankingCollector {

	private final KiwoomApiClient kiwoomApiClient;
	private final ProgramTradingRankingSnapshotRepository rankingRepository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		for (MarketType marketType : MarketType.values()) {
			for (ProgramRankingType rankingType : ProgramRankingType.values()) {
				for (AmtQtyType amtQtyType : AmtQtyType.values()) {
					try {
						collectForCombination(marketType, rankingType, amtQtyType, snapshotTime);
					} catch (Exception e) {
						log.error("프로그램매매 랭킹 수집 실패: market={}, ranking={}, amtQty={}",
							marketType, rankingType, amtQtyType, e);
					}
				}
			}
		}
	}

	private void collectForCombination(MarketType marketType, ProgramRankingType rankingType,
		AmtQtyType amtQtyType, LocalDateTime snapshotTime) {

		boolean alreadyExists = rankingRepository.existsBySnapshotTimeAndMarketTypeAndRankingTypeAndAmtQtyType(
			snapshotTime, marketType, rankingType, amtQtyType);
		if (alreadyExists) {
			log.debug("프로그램매매 랭킹 이미 존재, 스킵: market={}, ranking={}, amtQty={}, snapshotTime={}",
				marketType, rankingType, amtQtyType, snapshotTime);
			return;
		}

		var request = new Ka90003Request(rankingType.code(), amtQtyType.code(), Market.valueOf(marketType.name()).mrktTp, StexType.KRX_NXT.code());
		Ka90003Response response = kiwoomApiClient.post(request, Ka90003Response.class);

		List<Ka90003Response.RankingItem> items = response.items() != null ? response.items() : List.of();

		int rank = 1;
		for (Ka90003Response.RankingItem item : items) {
			// stk_cd에 "_AL" 또는 "_NX" suffix가 있으면 제거 (예: 000660_AL → 000660)
			String stockCode = stripAlSuffix(item.stkCd());
			if (stockCode.isBlank()) continue;

			String stockName = item.stkNm() != null ? item.stkNm().trim() : "";
			BigDecimal buyAmount = NumberParser.parseBigDecimal(item.prmBuyAmt());
			BigDecimal sellAmount = NumberParser.parseBigDecimal(item.prmSellAmt());
			BigDecimal netBuyAmount = NumberParser.parseBigDecimal(item.prmNetprpsAmt());

			rankingRepository.save(ProgramTradingRankingSnapshot.create(
				marketType, amtQtyType, rankingType, rank++,
				stockCode, stockName, buyAmount, sellAmount, netBuyAmount, snapshotTime
			));
		}

		log.debug("프로그램매매 랭킹 수집 완료: market={}, ranking={}, amtQty={}, count={}",
			marketType, rankingType, amtQtyType, rank - 1);
	}

	private enum Market {
		KOSPI("P00101"),
		KOSDAQ("P10102");
		final String mrktTp;
		Market(String mrktTp) { this.mrktTp = mrktTp; }
	}

	private static String stripAlSuffix(String stkCd) {
		if (stkCd == null) return "";
		String trimmed = stkCd.trim();
		// "_AL" 또는 "_NX" suffix 제거
		int underscoreIdx = trimmed.indexOf('_');
		return underscoreIdx >= 0 ? trimmed.substring(0, underscoreIdx) : trimmed;
	}
}
