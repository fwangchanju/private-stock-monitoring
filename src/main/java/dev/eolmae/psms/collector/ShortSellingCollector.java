package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.history.ShortSellingHistory;
import dev.eolmae.psms.domain.history.ShortSellingHistoryRepository;
import dev.eolmae.psms.domain.stock.WatchStockRepository;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.KiwoomResponseParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortSellingCollector {

	// ka10014: 공매도추이요청 (공매도 카테고리)
	private static final String API_PATH = "/api/dostk/shsa";
	private static final String TR_ID = "ka10014";
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
				log.error("공매도 이력 수집 실패: stockCode={}", stockCode, e);
			}
		}
	}

	private void collectForStock(String stockCode, LocalDate tradeDate) {
		if (shortSellingHistoryRepository.existsByStockCodeAndTradeDate(stockCode, tradeDate)) {
			log.debug("공매도 이력 이미 존재, 스킵: stockCode={}, tradeDate={}", stockCode, tradeDate);
			return;
		}

		String dateStr = tradeDate.format(DATE_FMT);

		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			TR_ID,
			Map.of(
				"stk_cd", stockCode,
				"tm_tp", "2",          // 2=일별
				"strt_dt", dateStr,
				"end_dt", dateStr
			)
		);

		// 응답 배열: shrts_trnsn
		JsonNode outputList = response.path("shrts_trnsn");
		for (JsonNode item : outputList) {
			long shortVolume = KiwoomResponseParser.parseLong(item, "shrts_qty");
			BigDecimal shortAmount = KiwoomResponseParser.parseBigDecimal(item, "shrts_trde_prica");
			BigDecimal shortRatio = KiwoomResponseParser.parseBigDecimal(item, "trde_wght");

			shortSellingHistoryRepository.save(
				ShortSellingHistory.create(stockCode, tradeDate, shortVolume, shortAmount, shortRatio)
			);
			break; // 당일 데이터 첫 번째 항목만 저장
		}

		log.debug("공매도 이력 수집 완료: stockCode={}, tradeDate={}", stockCode, tradeDate);
	}
}
