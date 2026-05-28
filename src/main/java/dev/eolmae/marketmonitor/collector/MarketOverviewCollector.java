package dev.eolmae.marketmonitor.collector;

import dev.eolmae.marketmonitor.common.enums.MarketType;
import dev.eolmae.marketmonitor.common.util.NumberParser;
import dev.eolmae.marketmonitor.domain.dashboard.MarketOverviewSnapshot;
import dev.eolmae.marketmonitor.domain.dashboard.repository.MarketOverviewSnapshotRepository;
import dev.eolmae.marketmonitor.external.kiwoom.client.KiwoomApiClient;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka20001Request;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka20001Response;
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
	private final MarketOverviewSnapshotRepository marketOverviewSnapshotRepository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		for (MarketType marketType : MarketType.storableValues()) {
			try {
				collectForMarket(marketType, snapshotTime);
			} catch (Exception e) {
				log.error("시장종합 수집 실패: market={}", marketType, e);
			}
		}
	}

	private void collectForMarket(MarketType marketType, LocalDateTime snapshotTime) {
		Market m = Market.valueOf(marketType.name());
		var request = new Ka20001Request(m.mrktTp, m.indsCd);
		var response = kiwoomApiClient.post(request, Ka20001Response.class);

		LocalDateTime now = LocalDateTime.now();

		BigDecimal indexValue = NumberParser.parseBigDecimal(response.curPrc());
		BigDecimal changeValue = NumberParser.parseBigDecimal(response.predPre());
		BigDecimal changeRate = NumberParser.parseBigDecimal(response.fluRt());
		BigDecimal tradingValue = NumberParser.parseBigDecimal(response.trdePrica());
		String marketStatus = response.mrktStatClsCode() != null ? response.mrktStatClsCode().trim() : "";
		int upperLimitCount = NumberParser.parseInt(response.upl());
		int lowerLimitCount = NumberParser.parseInt(response.lst());
		int advancers = NumberParser.parseInt(response.rising());
		int decliners = NumberParser.parseInt(response.fall());
		int unchangedCount = NumberParser.parseInt(response.stdns());

		if (marketOverviewSnapshotRepository.findByMarketTypeAndSnapshotTime(marketType, snapshotTime).isEmpty()) {
			marketOverviewSnapshotRepository.save(MarketOverviewSnapshot.create(
				marketType, snapshotTime, now, marketStatus, indexValue, changeValue, changeRate,
				tradingValue, upperLimitCount, lowerLimitCount, advancers, decliners, unchangedCount));
		}

		log.debug("시장종합 수집 완료: market={}, index={}", marketType, indexValue);
	}

	private enum Market {
		KOSPI("0", "001"),
		KOSDAQ("1", "101");
		final String mrktTp;  // ka20001 mrkt_tp
		final String indsCd;  // ka20001 inds_cd
		Market(String mrktTp, String indsCd) { this.mrktTp = mrktTp; this.indsCd = indsCd; }
	}
}
