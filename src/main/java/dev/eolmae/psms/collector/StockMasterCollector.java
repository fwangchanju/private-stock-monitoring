package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.stock.StockMaster;
import dev.eolmae.psms.domain.stock.StockMasterRepository;
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

	// plan.md 기준: ka10095, ka10099, ka10100 (종목 목록 조회)
	// TODO: 키움 Open API 포털에서 각 tr_id 용도 및 경로 확인 후 수정 필요
	// ka10095: KOSPI 종목 코드 목록
	// ka10099: KOSDAQ 종목 코드 목록
	// ka10100: 기타 (ETF 등) - 필요 시 사용
	private static final String API_PATH = "/api/dostk/stkinfo";
	private static final String TR_ID_KOSPI = "ka10095";
	private static final String TR_ID_KOSDAQ = "ka10099";

	private final KiwoomApiClient kiwoomApiClient;
	private final StockMasterRepository stockMasterRepository;

	@Transactional
	public void sync() {
		Set<String> fetchedCodes = new HashSet<>();

		fetchedCodes.addAll(syncForMarket(MarketType.KOSPI, TR_ID_KOSPI));
		fetchedCodes.addAll(syncForMarket(MarketType.KOSDAQ, TR_ID_KOSDAQ));

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

	private Set<String> syncForMarket(MarketType marketType, String trId) {
		Set<String> fetchedCodes = new HashSet<>();
		try {
			JsonNode response = kiwoomApiClient.post(
				API_PATH,
				trId,
				Map.of()  // TODO: 요청 파라미터 확인 필요 (없을 수도 있음)
			);

			// TODO: 응답 배열 필드명 확인 필요 (output 또는 output1)
			JsonNode outputList = response.path("output");
			for (JsonNode item : outputList) {
				// TODO: 아래 필드명들은 키움 API 응답 문서 기준으로 수정 필요
				String stockCode = KiwoomResponseParser.parseString(item, "stck_shrn_iscd");
				String stockName = KiwoomResponseParser.parseString(item, "hts_kor_isnm");

				if (stockCode.isEmpty()) {
					continue;
				}

				fetchedCodes.add(stockCode);

				StockMaster existing = stockMasterRepository.findById(stockCode).orElse(null);
				if (existing == null) {
					stockMasterRepository.save(StockMaster.create(stockCode, stockName, marketType));
				} else if (!existing.getStockName().equals(stockName) || !existing.isActive()) {
					existing.update(stockName, marketType);
				}
			}

			log.debug("종목 마스터 시장별 동기화 완료: market={}, count={}", marketType, fetchedCodes.size());
		} catch (Exception e) {
			log.error("종목 마스터 수집 실패: market={}", marketType, e);
		}
		return fetchedCodes;
	}
}
