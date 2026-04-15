package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.stock.StockMaster;
import dev.eolmae.psms.domain.stock.StockMasterRepository;
import dev.eolmae.psms.exception.EscalateException;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.KiwoomResponseParser;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockMasterCollector {

	// ka10099: 종목정보 리스트 (종목정보 카테고리)
	// mrkt_tp 파라미터로 시장 구분: 0=코스피, 10=코스닥
	private static final String API_PATH = "/api/dostk/stkinfo";
	private static final String TR_ID = "ka10099";

	private final KiwoomApiClient kiwoomApiClient;
	private final StockMasterRepository stockMasterRepository;

	@Transactional
	public void sync() {
		Set<String> fetchedCodes = new HashSet<>();

		try {
			fetchedCodes.addAll(syncForMarket(MarketType.KOSPI, "0"));
			fetchedCodes.addAll(syncForMarket(MarketType.KOSDAQ, "10"));
		} catch (EscalateException e) {
			throw e;
		} catch (Exception e) {
			throw new EscalateException("종목 마스터 동기화 실패 — API 호출 불가", e);
		}

		// API에서 더 이상 조회되지 않는 종목은 비활성화
		List<StockMaster> allStocks = stockMasterRepository.findAll();
		for (StockMaster stock : allStocks) {
			if (stock.isActive() && !fetchedCodes.contains(stock.getStockCode())) {
				stock.markInactive();
				log.debug("종목 비활성화: stockCode={}, stockName={}", stock.getStockCode(), stock.getStockName());
			}
		}

		log.info("종목 마스터 동기화 완료: 조회 종목 수={}", fetchedCodes.size());
	}

	private Set<String> syncForMarket(MarketType marketType, String mrktTp) {
		Set<String> fetchedCodes = new HashSet<>();

		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			TR_ID,
			Map.of("mrkt_tp", mrktTp)
		);

		// 응답 래퍼 필드: list
		JsonNode outputList = response.path("list");
		for (JsonNode item : outputList) {
			String stockCode = KiwoomResponseParser.parseString(item, "code");
			String rawName = KiwoomResponseParser.parseString(item, "name");

			if (stockCode.isEmpty()) {
				continue;
			}

			// 키움 API가 종목명에 공백을 포함해 반환하는 경우가 있어 제거
			String stockName = rawName.replace(" ", "");
			fetchedCodes.add(stockCode);

			StockMaster existing = stockMasterRepository.findById(stockCode).orElse(null);
			if (existing == null) {
				stockMasterRepository.save(StockMaster.create(stockCode, stockName, marketType));
			} else if (!existing.getStockName().equals(stockName) || !existing.isActive()) {
				existing.update(stockName, marketType);
			}
		}

		log.debug("종목 마스터 시장별 동기화 완료: market={}, count={}", marketType, fetchedCodes.size());
		return fetchedCodes;
	}
}
