package dev.eolmae.marketmonitor.collector;

import dev.eolmae.marketmonitor.common.enums.MarketType;
import dev.eolmae.marketmonitor.domain.stock.StockMaster;
import dev.eolmae.marketmonitor.domain.stock.repository.StockMasterRepository;
import dev.eolmae.marketmonitor.exception.EscalateException;
import dev.eolmae.marketmonitor.external.kiwoom.client.KiwoomApiClient;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka10099Request;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka10099Response;
import java.util.HashSet;
import java.util.List;
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

	private final KiwoomApiClient kiwoomApiClient;
	private final StockMasterRepository stockMasterRepository;

	@Transactional
	public void sync() {
		Set<String> fetchedCodes = new HashSet<>();

		try {
			for (MarketType marketType : MarketType.storableValues()) {
				fetchedCodes.addAll(syncForMarket(marketType));
			}
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

	private Set<String> syncForMarket(MarketType marketType) {
		Set<String> fetchedCodes = new HashSet<>();

		var request = new Ka10099Request(Market.valueOf(marketType.name()).mrktTp);
		Ka10099Response response = kiwoomApiClient.post(request, Ka10099Response.class);

		if (response.list() == null) {
			return fetchedCodes;
		}

		for (Ka10099Response.StockItem item : response.list()) {
			String stockCode = item.code() != null ? item.code().trim() : "";
			if (stockCode.isEmpty()) {
				continue;
			}

			// 키움 API가 종목명에 공백을 포함해 반환하는 경우가 있어 제거
			String stockName = item.name() != null ? item.name().replace(" ", "") : "";
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

	private enum Market {
		KOSPI("0"),
		KOSDAQ("10");
		final String mrktTp;  // ka10099 mrkt_tp (ka20001과 코드 체계 다름)
		Market(String mrktTp) { this.mrktTp = mrktTp; }
	}
}
