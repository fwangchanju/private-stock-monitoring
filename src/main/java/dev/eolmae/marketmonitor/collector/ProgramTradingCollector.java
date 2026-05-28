package dev.eolmae.marketmonitor.collector;

import dev.eolmae.marketmonitor.common.enums.StexType;
import dev.eolmae.marketmonitor.common.util.NumberParser;
import dev.eolmae.marketmonitor.domain.history.ProgramTradingDailyHistory;
import dev.eolmae.marketmonitor.domain.history.repository.ProgramTradingDailyHistoryRepository;
import dev.eolmae.marketmonitor.domain.history.ProgramTradingHistory;
import dev.eolmae.marketmonitor.domain.history.repository.ProgramTradingHistoryRepository;
import dev.eolmae.marketmonitor.domain.stock.repository.WatchStockRepository;
import dev.eolmae.marketmonitor.external.kiwoom.client.KiwoomApiClient;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka90008Request;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka90008Response;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka90013Request;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka90013Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// ka90008: 종목시간별프로그램매매추이 / ka90013: 종목일별프로그램매매추이
// ka90008: 각 틱은 해당 마켓(KRX/NXT)의 당일 누적합 → KRX 최신 틱 + NXT 최신 틱 합산값만 저장
// ka90013: 각 틱은 해당 날짜의 일별 합계 → KRX[date] + NXT[date] 합산 저장
// 순매수금액은 prm_netprps_amt 미사용 (-- 파싱 오류) → buy - sell 직접 계산
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramTradingCollector {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final KiwoomApiClient kiwoomApiClient;
	private final ProgramTradingHistoryRepository historyRepository;
	private final ProgramTradingDailyHistoryRepository dailyHistoryRepository;
	private final WatchStockRepository watchStockRepository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		List<String> stockCodes = watchStockRepository.findDistinctStockCodes();
		for (String stockCode : stockCodes) {
			try {
				collectIntradayForStock(stockCode, snapshotTime);
			} catch (Exception e) {
				log.error("프로그램매매 장중이력 수집 실패: stockCode={}", stockCode, e);
			}
		}
	}

	@Transactional
	public void collectDaily(LocalDate tradeDate) {
		List<String> stockCodes = watchStockRepository.findDistinctStockCodes();
		for (String stockCode : stockCodes) {
			try {
				collectDailyForStock(stockCode, tradeDate);
			} catch (Exception e) {
				log.error("프로그램매매 일별이력 수집 실패: stockCode={}", stockCode, e);
			}
		}
	}

	private void collectIntradayForStock(String stockCode, LocalDateTime snapshotTime) {
		if (historyRepository.existsByStockCodeAndSnapshotTime(stockCode, snapshotTime)) {
			return;
		}

		String dateStr = snapshotTime.format(DATE_FMT);

		// ka90008: 각 틱은 당일 누적합 → KRX·NXT 각각 최신 틱(리스트 마지막)만 사용
		var krxRequest = new Ka90008Request(stockCode, StexType.KRX.code(), dateStr);
		Ka90008Response krxResponse = kiwoomApiClient.post(krxRequest, Ka90008Response.class);

		var nxtRequest = new Ka90008Request(stockCode + "_NX", StexType.KRX.code(), dateStr);
		Ka90008Response nxtResponse = kiwoomApiClient.post(nxtRequest, Ka90008Response.class);

		List<Ka90008Response.TradeTick> krxTicks = krxResponse.ticks() != null ? krxResponse.ticks() : List.of();
		List<Ka90008Response.TradeTick> nxtTicks = nxtResponse.ticks() != null ? nxtResponse.ticks() : List.of();

		if (krxTicks.isEmpty() && nxtTicks.isEmpty()) {
			log.debug("프로그램매매 장중이력 없음: stockCode={}", stockCode);
			return;
		}

		Ka90008Response.TradeTick krxLatest = krxTicks.isEmpty() ? null : krxTicks.get(krxTicks.size() - 1);
		Ka90008Response.TradeTick nxtLatest = nxtTicks.isEmpty() ? null : nxtTicks.get(nxtTicks.size() - 1);

		BigDecimal buyAmt = BigDecimal.ZERO;
		BigDecimal sellAmt = BigDecimal.ZERO;

		if (krxLatest != null) {
			buyAmt = buyAmt.add(NumberParser.parseBigDecimal(krxLatest.prmBuyAmt()));
			sellAmt = sellAmt.add(NumberParser.parseBigDecimal(krxLatest.prmSellAmt()));
		}
		if (nxtLatest != null) {
			buyAmt = buyAmt.add(NumberParser.parseBigDecimal(nxtLatest.prmBuyAmt()));
			sellAmt = sellAmt.add(NumberParser.parseBigDecimal(nxtLatest.prmSellAmt()));
		}

		BigDecimal netBuyAmt = buyAmt.subtract(sellAmt);

		historyRepository.save(ProgramTradingHistory.create(stockCode, snapshotTime, buyAmt, sellAmt, netBuyAmt));

		log.debug("프로그램매매 장중이력 수집 완료: stockCode={}, buy={}, sell={}, net={}",
			stockCode, buyAmt, sellAmt, netBuyAmt);
	}

	private void collectDailyForStock(String stockCode, LocalDate tradeDate) {
		// ka90013: 각 틱은 해당 날짜의 일별 집계 → KRX[date] + NXT[date] 합산
		var krxRequest = new Ka90013Request(stockCode, StexType.KRX.code());
		Ka90013Response krxResponse = kiwoomApiClient.post(krxRequest, Ka90013Response.class);

		var nxtRequest = new Ka90013Request(stockCode + "_NX", StexType.KRX.code());
		Ka90013Response nxtResponse = kiwoomApiClient.post(nxtRequest, Ka90013Response.class);

		List<Ka90013Response.DailyTick> krxTicks = krxResponse.ticks() != null ? krxResponse.ticks() : List.of();
		List<Ka90013Response.DailyTick> nxtTicks = nxtResponse.ticks() != null ? nxtResponse.ticks() : List.of();

		// date → [buy, sell] 합산 (net = buy - sell)
		Map<String, BigDecimal[]> merged = new HashMap<>();

		for (Ka90013Response.DailyTick tick : krxTicks) {
			String dt = tick.dt() != null ? tick.dt().trim() : null;
			if (dt == null || dt.isBlank()) continue;
			accumulateDaily(merged, dt, tick.prmBuyAmt(), tick.prmSellAmt());
		}
		for (Ka90013Response.DailyTick tick : nxtTicks) {
			String dt = tick.dt() != null ? tick.dt().trim() : null;
			if (dt == null || dt.isBlank()) continue;
			accumulateDaily(merged, dt, tick.prmBuyAmt(), tick.prmSellAmt());
		}

		for (Map.Entry<String, BigDecimal[]> entry : merged.entrySet()) {
			LocalDate date = parseDate(entry.getKey());
			if (date == null) continue;
			if (dailyHistoryRepository.existsByStockCodeAndTradeDate(stockCode, date)) continue;
			BigDecimal buy = entry.getValue()[0];
			BigDecimal sell = entry.getValue()[1];
			dailyHistoryRepository.save(ProgramTradingDailyHistory.create(
				stockCode, date, buy, sell, buy.subtract(sell)
			));
		}

		log.debug("프로그램매매 일별이력 수집 완료: stockCode={}", stockCode);
	}

	private static void accumulateDaily(Map<String, BigDecimal[]> merged, String dt,
		String buyAmt, String sellAmt) {
		var amounts = merged.computeIfAbsent(dt, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
		amounts[0] = amounts[0].add(NumberParser.parseBigDecimal(buyAmt));
		amounts[1] = amounts[1].add(NumberParser.parseBigDecimal(sellAmt));
	}

	private static LocalDate parseDate(String dt) {
		try {
			return LocalDate.of(
				Integer.parseInt(dt.substring(0, 4)),
				Integer.parseInt(dt.substring(4, 6)),
				Integer.parseInt(dt.substring(6, 8))
			);
		} catch (Exception e) {
			return null;
		}
	}
}
