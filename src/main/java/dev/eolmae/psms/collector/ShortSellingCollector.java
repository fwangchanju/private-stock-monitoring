package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.history.ShortSellingHistory;
import dev.eolmae.psms.domain.history.ShortSellingHistoryRepository;
import dev.eolmae.psms.domain.stock.WatchStockRepository;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.KiwoomResponseParser;
import java.math.BigDecimal;
import java.time.LocalDate;
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

	// plan.md 기준: kt50075 (공매도 추이)
	// TODO: 키움 Open API 포털에서 경로 및 tr_id 확인 후 수정 필요
	private static final String API_PATH = "/api/dostk/stkinfo";
	private static final String TR_ID = "kt50075";

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

		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			TR_ID,
			Map.of("stck_shrn_iscd", stockCode)  // TODO: 파라미터명 확인 필요
		);

		// TODO: 응답 구조 확인 필요. 일별 공매도 목록이면 output1 배열, 당일 단건이면 output 등
		JsonNode outputList = response.path("output");
		for (JsonNode item : outputList) {
			// TODO: 아래 필드명들은 키움 API 응답 문서 기준으로 수정 필요
			// TODO: API가 여러 날짜 데이터를 반환하는 경우 날짜 파싱 후 tradeDate 필터링 필요
			long shortVolume = KiwoomResponseParser.parseLong(item, "sln_qty");
			BigDecimal shortAmount = KiwoomResponseParser.parseBigDecimal(item, "sln_tr_pbmn");
			BigDecimal shortRatio = KiwoomResponseParser.parseBigDecimal(item, "seln_tr_rt");

			shortSellingHistoryRepository.save(
				ShortSellingHistory.create(stockCode, tradeDate, shortVolume, shortAmount, shortRatio)
			);

			// 당일 데이터 하나만 저장 (API가 여러 일자를 반환하는 경우 첫 번째만)
			break;
		}

		log.debug("공매도 이력 수집 완료: stockCode={}, tradeDate={}", stockCode, tradeDate);
	}
}
