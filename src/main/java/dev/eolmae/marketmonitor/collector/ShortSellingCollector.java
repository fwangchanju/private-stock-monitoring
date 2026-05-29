package dev.eolmae.marketmonitor.collector;

import dev.eolmae.marketmonitor.common.util.NumberParser;
import dev.eolmae.marketmonitor.domain.history.ShortSellingDailyHistory;
import dev.eolmae.marketmonitor.domain.history.ShortSellingSnapshot;
import dev.eolmae.marketmonitor.domain.history.repository.ShortSellingDailyHistoryRepository;
import dev.eolmae.marketmonitor.domain.history.repository.ShortSellingSnapshotRepository;
import dev.eolmae.marketmonitor.domain.stock.repository.WatchStockRepository;
import dev.eolmae.marketmonitor.external.kiwoom.client.KiwoomApiClient;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka10014Request;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka10014Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// ka10014: 공매도추이요청
// 스케줄러: strt_dt = today, 오늘 데이터만 → short_selling_snapshot
// 백필: strt_dt = today-60, 과거 → short_selling_daily, 오늘 → short_selling_snapshot
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortSellingCollector {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final String TM_TP_DAILY = "2"; // ka10014 tm_tp: 2=일별

	private final KiwoomApiClient kiwoomApiClient;
	private final ShortSellingDailyHistoryRepository dailyRepository;
	private final ShortSellingSnapshotRepository snapshotRepository;
	private final WatchStockRepository watchStockRepository;

	/** 스케줄러 호출 — 오늘 데이터만 요청, short_selling_snapshot에 적재 */
	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		LocalDate today = snapshotTime.toLocalDate();
		String todayStr = today.format(DATE_FMT);

		List<String> stockCodes = watchStockRepository.findDistinctStockCodes();
		for (String stockCode : stockCodes) {
			try {
				collectForStock(stockCode, today, snapshotTime, todayStr, todayStr);
			} catch (Exception e) {
				log.error("공매도 수집 실패: stockCode={}", stockCode, e);
			}
		}
	}

	/** 관심종목 신규 등록 시 백필 — today-60일부터 수집, 비동기 호출 */
	@Transactional
	public void backfill(String stockCode, LocalDateTime snapshotTime) {
		LocalDate today = snapshotTime.toLocalDate();
		String endDt = today.format(DATE_FMT);
		String startDt = today.minusDays(60).format(DATE_FMT);
		collectForStock(stockCode, today, snapshotTime, startDt, endDt);
		log.info("공매도 백필 완료: stockCode={}", stockCode);
	}

	private void collectForStock(String stockCode, LocalDate today, LocalDateTime snapshotTime,
		String startDt, String endDt) {

		var request = new Ka10014Request(stockCode, TM_TP_DAILY, startDt, endDt);
		Ka10014Response response = kiwoomApiClient.post(request, Ka10014Response.class);

		if (response.ticks() == null || response.ticks().isEmpty()) {
			log.debug("공매도 데이터 없음: stockCode={}", stockCode);
			return;
		}

		for (Ka10014Response.ShortTick tick : response.ticks()) {
			if (tick.dt() == null || tick.dt().isBlank()) continue;

			LocalDate tradeDate = parseDate(tick.dt());
			if (tradeDate == null) continue;

			BigDecimal closePrice = NumberParser.parseBigDecimal(tick.closePric());
			BigDecimal priceChange = NumberParser.parseBigDecimal(tick.predPre());
			BigDecimal changeRate = NumberParser.parseBigDecimal(tick.fluRt());
			long tradingVolume = NumberParser.parseLong(tick.trdeQty());
			long shortVolume = NumberParser.parseLong(tick.shrtsQty());
			long cumulativeShortVolume = NumberParser.parseLong(tick.ovrShrtsQty());
			BigDecimal shortRatio = NumberParser.parseBigDecimal(tick.trdeWght());
			BigDecimal shortAmount = NumberParser.parseBigDecimal(tick.shrtsTrdePrica());
			BigDecimal shortAvgPrice = NumberParser.parseBigDecimal(tick.shrtsAvgPric());

			if (tradeDate.isBefore(today)) {
				if (!dailyRepository.existsByStockCodeAndTradeDate(stockCode, tradeDate)) {
					dailyRepository.save(ShortSellingDailyHistory.create(
						stockCode, tradeDate,
						closePrice, priceChange, changeRate,
						tradingVolume, shortVolume, cumulativeShortVolume,
						shortRatio, shortAmount, shortAvgPrice));
				}
			} else {
				if (!snapshotRepository.existsByStockCodeAndSnapshotTime(stockCode, snapshotTime)) {
					snapshotRepository.save(ShortSellingSnapshot.create(
						stockCode, tradeDate, snapshotTime,
						closePrice, priceChange, changeRate,
						tradingVolume, shortVolume, cumulativeShortVolume,
						shortRatio, shortAmount, shortAvgPrice));
				}
			}
		}

		log.debug("공매도 수집 완료: stockCode={}", stockCode);
	}

	private static LocalDate parseDate(String dt) {
		try {
			return LocalDate.parse(dt.trim(), DATE_FMT);
		} catch (Exception e) {
			return null;
		}
	}
}
