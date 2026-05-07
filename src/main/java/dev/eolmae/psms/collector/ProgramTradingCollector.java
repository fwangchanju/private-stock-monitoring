package dev.eolmae.psms.collector;

import dev.eolmae.psms.domain.history.ProgramTradingDailyHistory;
import dev.eolmae.psms.domain.history.ProgramTradingDailyHistoryRepository;
import dev.eolmae.psms.domain.history.ProgramTradingHistory;
import dev.eolmae.psms.domain.history.ProgramTradingHistoryRepository;
import dev.eolmae.psms.domain.stock.WatchStockRepository;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.dto.Ka90008Request;
import dev.eolmae.psms.external.kiwoom.dto.Ka90008Response;
import dev.eolmae.psms.external.kiwoom.dto.Ka90013Request;
import dev.eolmae.psms.external.kiwoom.dto.Ka90013Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// ka90008: 종목시간별프로그램매매추이 / ka90013: 종목일별프로그램매매추이
// 각 종목별 KRX + NXT 2회 호출 후 합산
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
		String dateStr = snapshotTime.format(DATE_FMT);

		var krxRequest = new Ka90008Request(stockCode, "1", dateStr);
		Ka90008Response krxResponse = kiwoomApiClient.post(krxRequest, Ka90008Response.class);

		var nxtRequest = new Ka90008Request(stockCode + "_NX", "1", dateStr);
		Ka90008Response nxtResponse = kiwoomApiClient.post(nxtRequest, Ka90008Response.class);

		List<Ka90008Response.TradeTick> krxTicks = krxResponse.ticks() != null ? krxResponse.ticks() : List.of();
		List<Ka90008Response.TradeTick> nxtTicks = nxtResponse.ticks() != null ? nxtResponse.ticks() : List.of();

		// tm 기준 분 단위 버킷(예: 143433 → 143400)으로 KRX+NXT 합산
		// key=분버킷, value=[buyAmt, sellAmt, netAmt]
		Map<String, BigDecimal[]> merged = new LinkedHashMap<>();

		for (Ka90008Response.TradeTick tick : krxTicks) {
			String bucket = toMinuteBucket(tick.tm());
			if (bucket == null) continue;
			var amounts = merged.computeIfAbsent(bucket, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
			amounts[0] = amounts[0].add(parseAmount(tick.prmBuyAmt()));
			amounts[1] = amounts[1].add(parseAmount(tick.prmSellAmt()));
			amounts[2] = amounts[2].add(parseAmount(tick.prmNetprpsAmt()));
		}
		for (Ka90008Response.TradeTick tick : nxtTicks) {
			String bucket = toMinuteBucket(tick.tm());
			if (bucket == null) continue;
			var amounts = merged.computeIfAbsent(bucket, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
			amounts[0] = amounts[0].add(parseAmount(tick.prmBuyAmt()));
			amounts[1] = amounts[1].add(parseAmount(tick.prmSellAmt()));
			amounts[2] = amounts[2].add(parseAmount(tick.prmNetprpsAmt()));
		}

		if (merged.isEmpty()) {
			log.debug("프로그램매매 장중이력 없음: stockCode={}", stockCode);
			return;
		}

		for (Map.Entry<String, BigDecimal[]> entry : merged.entrySet()) {
			LocalDateTime recordTime = parseSnapshotTime(dateStr, entry.getKey());
			if (recordTime == null) continue;
			if (historyRepository.existsByStockCodeAndSnapshotTime(stockCode, recordTime)) continue;
			BigDecimal[] amounts = entry.getValue();
			historyRepository.save(ProgramTradingHistory.create(
				stockCode, recordTime, amounts[0], amounts[1], amounts[2]
			));
		}

		log.debug("프로그램매매 장중이력 수집 완료: stockCode={}, buckets={}", stockCode, merged.size());
	}

	private void collectDailyForStock(String stockCode, LocalDate tradeDate) {
		var krxRequest = new Ka90013Request(stockCode, "1");
		Ka90013Response krxResponse = kiwoomApiClient.post(krxRequest, Ka90013Response.class);

		var nxtRequest = new Ka90013Request(stockCode + "_NX", "1");
		Ka90013Response nxtResponse = kiwoomApiClient.post(nxtRequest, Ka90013Response.class);

		List<Ka90013Response.DailyTick> krxTicks = krxResponse.ticks() != null ? krxResponse.ticks() : List.of();
		List<Ka90013Response.DailyTick> nxtTicks = nxtResponse.ticks() != null ? nxtResponse.ticks() : List.of();

		Map<String, BigDecimal[]> merged = new LinkedHashMap<>();

		for (Ka90013Response.DailyTick tick : krxTicks) {
			String dt = tick.dt() != null ? tick.dt().trim() : null;
			if (dt == null || dt.isBlank()) continue;
			var amounts = merged.computeIfAbsent(dt, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
			amounts[0] = amounts[0].add(parseAmount(tick.prmBuyAmt()));
			amounts[1] = amounts[1].add(parseAmount(tick.prmSellAmt()));
			amounts[2] = amounts[2].add(parseAmount(tick.prmNetprpsAmt()));
		}
		for (Ka90013Response.DailyTick tick : nxtTicks) {
			String dt = tick.dt() != null ? tick.dt().trim() : null;
			if (dt == null || dt.isBlank()) continue;
			var amounts = merged.computeIfAbsent(dt, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
			amounts[0] = amounts[0].add(parseAmount(tick.prmBuyAmt()));
			amounts[1] = amounts[1].add(parseAmount(tick.prmSellAmt()));
			amounts[2] = amounts[2].add(parseAmount(tick.prmNetprpsAmt()));
		}

		for (Map.Entry<String, BigDecimal[]> entry : merged.entrySet()) {
			LocalDate date = parseDate(entry.getKey());
			if (date == null) continue;
			if (dailyHistoryRepository.existsByStockCodeAndTradeDate(stockCode, date)) continue;
			BigDecimal[] amounts = entry.getValue();
			dailyHistoryRepository.save(ProgramTradingDailyHistory.create(
				stockCode, date, amounts[0], amounts[1], amounts[2]
			));
		}

		log.debug("프로그램매매 일별이력 수집 완료: stockCode={}", stockCode);
	}

	// 분 단위 버킷 변환: "143433" → "143400"
	private static String toMinuteBucket(String tm) {
		if (tm == null || tm.trim().length() != 6) return null;
		return tm.trim().substring(0, 4) + "00";
	}

	// "20240101" + "143400" → LocalDateTime(2024-01-01T14:34:00)
	private static LocalDateTime parseSnapshotTime(String dateStr, String tmBucket) {
		try {
			return LocalDateTime.of(
				Integer.parseInt(dateStr.substring(0, 4)),
				Integer.parseInt(dateStr.substring(4, 6)),
				Integer.parseInt(dateStr.substring(6, 8)),
				Integer.parseInt(tmBucket.substring(0, 2)),
				Integer.parseInt(tmBucket.substring(2, 4)),
				0
			);
		} catch (Exception e) {
			return null;
		}
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

	private static BigDecimal parseAmount(String value) {
		if (value == null || value.isBlank() || "-".equals(value.trim())) return BigDecimal.ZERO;
		String cleaned = value.replace(",", "").trim();
		if (cleaned.startsWith("--")) cleaned = cleaned.substring(1);
		try { return new BigDecimal(cleaned); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
	}
}
