package dev.eolmae.psms.collector;

import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.dashboard.MarketOverview;
import dev.eolmae.psms.domain.dashboard.MarketOverviewRepository;
import dev.eolmae.psms.domain.dashboard.MarketOverviewSnapshot;
import dev.eolmae.psms.domain.dashboard.MarketOverviewSnapshotRepository;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.dto.Ka20001Request;
import dev.eolmae.psms.external.kiwoom.dto.Ka20001Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketOverviewCollector {

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
		// mrkt_tp: 0=코스피, 1=코스닥 / inds_cd: 001=코스피종합, 101=코스닥
		String mrktTp = marketType == MarketType.KOSPI ? "0" : "1";
		String indsCd = marketType == MarketType.KOSPI ? "001" : "101";

		var request = new Ka20001Request(mrktTp, indsCd);
		var response = kiwoomApiClient.post(request, Ka20001Response.class);

		LocalDateTime now = LocalDateTime.now();

		BigDecimal indexValue = parseAmount(response.curPrc());
		BigDecimal changeValue = parseAmount(response.predPre());
		BigDecimal changeRate = parseAmount(response.fluRt());
		BigDecimal tradingValue = parseAmount(response.trdePrica());
		String marketStatus = response.mrktStatClsCode() != null ? response.mrktStatClsCode().trim() : "";
		int upperLimitCount = parseCount(response.upl());
		int lowerLimitCount = parseCount(response.lst());
		int advancers = parseCount(response.rising());
		int decliners = parseCount(response.fall());
		int unchangedCount = parseCount(response.stdns());

		MarketOverview overview = marketOverviewRepository.findByMarketType(marketType)
			.map(existing -> {
				existing.update(snapshotTime, now, marketStatus, indexValue, changeValue,
					changeRate, tradingValue, upperLimitCount, lowerLimitCount,
					advancers, decliners, unchangedCount);
				return existing;
			})
			.orElseGet(() -> MarketOverview.create(marketType, snapshotTime, now, marketStatus,
				indexValue, changeValue, changeRate, tradingValue,
				upperLimitCount, lowerLimitCount, advancers, decliners, unchangedCount));

		marketOverviewRepository.save(overview);

		if (marketOverviewSnapshotRepository.findByMarketTypeAndSnapshotTime(marketType, snapshotTime).isEmpty()) {
			marketOverviewSnapshotRepository.save(MarketOverviewSnapshot.from(overview));
		}

		log.debug("시장종합 수집 완료: market={}, index={}", marketType, indexValue);
	}

	private static BigDecimal parseAmount(String value) {
		if (value == null || value.isBlank() || "-".equals(value.trim())) return BigDecimal.ZERO;
		try {
			return new BigDecimal(value.replace(",", "").trim());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private static int parseCount(String value) {
		if (value == null || value.isBlank() || "-".equals(value.trim())) return 0;
		try {
			return Integer.parseInt(value.replace(",", "").trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
