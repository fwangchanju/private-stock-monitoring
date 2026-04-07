package dev.eolmae.psms.collector;

import dev.eolmae.psms.domain.history.ShortSellingHistory;
import dev.eolmae.psms.domain.history.ShortSellingHistoryRepository;
import dev.eolmae.psms.domain.stock.WatchStockRepository;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.dto.Ka10014Request;
import dev.eolmae.psms.external.kiwoom.dto.Ka10014Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// ka10014: 공매도추이요청 — 관심종목별 KRX + NXT 각 1회 호출 후 합산
// 수집 주기: 평일 19:00 (당일 자료 18:30 이후 제공)
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortSellingCollector {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final KiwoomApiClient kiwoomApiClient;
	private final ShortSellingHistoryRepository shortSellingHistoryRepository;
	private final WatchStockRepository watchStockRepository;

	@Transactional
	public void collect(LocalDate tradeDate) {
		List<String> stockCodes = watchStockRepository.findDistinctStockCodes();
		for (String stockCode : stockCodes) {
			try {
				collectForStock(stockCode, tradeDate);
			} catch (Exception e) {
				log.error("공매도 이력 수집 실패, 재시도 시작: stockCode={}", stockCode, e);
				try {
					collectForStock(stockCode, tradeDate);
				} catch (Exception retryException) {
					log.error("공매도 이력 수집 재시도 실패, 스킵: stockCode={}", stockCode, retryException);
				}
			}
		}
	}

	private void collectForStock(String stockCode, LocalDate tradeDate) {
		String endDt = tradeDate.format(DATE_FMT);
		String startDt = tradeDate.minusMonths(2).format(DATE_FMT);

		// KRX + NXT 각 1회 호출 (2개월치 일별 데이터)
		var krxRequest = new Ka10014Request(stockCode, "2", startDt, endDt);
		Ka10014Response krxResponse = kiwoomApiClient.post(krxRequest, Ka10014Response.class);

		var nxtRequest = new Ka10014Request(stockCode + "_NX", "2", startDt, endDt);
		Ka10014Response nxtResponse = kiwoomApiClient.post(nxtRequest, Ka10014Response.class);

		List<Ka10014Response.ShortTick> krxTicks = krxResponse.ticks() != null ? krxResponse.ticks() : List.of();
		List<Ka10014Response.ShortTick> nxtTicks = nxtResponse.ticks() != null ? nxtResponse.ticks() : List.of();

		// dt 기준 합산: shrts_qty, ovr_shrts_qty, trde_qty, shrts_trde_prica는 KRX+NXT 합산
		// close_pric, pred_pre, flu_rt는 KRX 값 사용 (NXT는 참고하지 않음)
		record DayData(
			BigDecimal shrtsQty, BigDecimal ovrShrtsQty,
			BigDecimal shrtsTrdePrica, BigDecimal trdeQty,
			BigDecimal closePric, BigDecimal predPre, BigDecimal fluRt
		) {}

		Map<String, DayData> merged = new LinkedHashMap<>();

		for (Ka10014Response.ShortTick tick : krxTicks) {
			String dt = tick.dt() != null ? tick.dt().trim() : null;
			if (dt == null || dt.isBlank()) continue;
			merged.put(dt, new DayData(
				parseAmount(tick.shrtsQty()),
				parseAmount(tick.ovrShrtsQty()),
				parseAmount(tick.shrtsTrdePrica()),
				parseAmount(tick.trdeQty()),
				parseAmount(tick.closePric()),
				parseAmount(tick.predPre()),
				parseAmount(tick.fluRt())
			));
		}
		for (Ka10014Response.ShortTick tick : nxtTicks) {
			String dt = tick.dt() != null ? tick.dt().trim() : null;
			if (dt == null || dt.isBlank()) continue;
			DayData existing = merged.get(dt);
			if (existing == null) {
				merged.put(dt, new DayData(
					parseAmount(tick.shrtsQty()),
					parseAmount(tick.ovrShrtsQty()),
					parseAmount(tick.shrtsTrdePrica()),
					parseAmount(tick.trdeQty()),
					parseAmount(tick.closePric()),
					parseAmount(tick.predPre()),
					parseAmount(tick.fluRt())
				));
			} else {
				// NXT 합산: 수량/금액은 합산, 가격정보는 KRX 유지
				merged.put(dt, new DayData(
					existing.shrtsQty().add(parseAmount(tick.shrtsQty())),
					existing.ovrShrtsQty().add(parseAmount(tick.ovrShrtsQty())),
					existing.shrtsTrdePrica().add(parseAmount(tick.shrtsTrdePrica())),
					existing.trdeQty().add(parseAmount(tick.trdeQty())),
					existing.closePric(),
					existing.predPre(),
					existing.fluRt()
				));
			}
		}

		for (Map.Entry<String, DayData> entry : merged.entrySet()) {
			LocalDate date = parseDate(entry.getKey());
			if (date == null) continue;

			DayData d = entry.getValue();

			// shrts_avg_pric 재계산: shrts_trde_prica / shrts_qty
			BigDecimal shrtsAvgPric = BigDecimal.ZERO;
			if (d.shrtsQty().compareTo(BigDecimal.ZERO) > 0) {
				shrtsAvgPric = d.shrtsTrdePrica().divide(d.shrtsQty(), 4, RoundingMode.HALF_UP);
			}

			// 공매도비율(trde_wght): shrts_qty / trde_qty * 100
			BigDecimal shortRatio = BigDecimal.ZERO;
			if (d.trdeQty().compareTo(BigDecimal.ZERO) > 0) {
				shortRatio = d.shrtsQty().divide(d.trdeQty(), 4, RoundingMode.HALF_UP)
					.multiply(BigDecimal.valueOf(100));
			}

			var existing = shortSellingHistoryRepository.findByStockCodeAndTradeDate(stockCode, date);
			if (existing.isPresent()) {
				existing.get().update(
					d.shrtsQty().longValue(), d.ovrShrtsQty().longValue(),
					d.shrtsTrdePrica(), shrtsAvgPric, shortRatio,
					d.closePric(), d.predPre(), d.fluRt()
				);
			} else {
				shortSellingHistoryRepository.save(ShortSellingHistory.create(
					stockCode, date,
					d.shrtsQty().longValue(), d.ovrShrtsQty().longValue(),
					d.shrtsTrdePrica(), shrtsAvgPric, shortRatio,
					d.closePric(), d.predPre(), d.fluRt()
				));
			}
		}

		log.debug("공매도 이력 수집 완료: stockCode={}, dates={}", stockCode, merged.size());
	}

	private static BigDecimal parseAmount(String value) {
		if (value == null || value.isBlank() || "-".equals(value.trim())) return BigDecimal.ZERO;
		String cleaned = value.replace(",", "").trim();
		if (cleaned.startsWith("--")) cleaned = cleaned.substring(1);
		try { return new BigDecimal(cleaned); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
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
