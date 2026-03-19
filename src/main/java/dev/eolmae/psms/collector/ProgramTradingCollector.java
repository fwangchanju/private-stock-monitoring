package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.common.ProgramRankingType;
import dev.eolmae.psms.domain.dashboard.ProgramTradingRankingSnapshot;
import dev.eolmae.psms.domain.dashboard.ProgramTradingRankingSnapshotRepository;
import dev.eolmae.psms.domain.history.ProgramTradingHistory;
import dev.eolmae.psms.domain.history.ProgramTradingHistoryRepository;
import dev.eolmae.psms.domain.stock.WatchStockRepository;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.KiwoomResponseParser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramTradingCollector {

	// TODO: 키움 Open API 포털에서 확인 후 수정 필요
	// 프로그램매매 순매수/순매도 상위 종목 랭킹 API
	private static final String RANKING_API_PATH = "/api/dostk/programtrade";
	private static final String RANKING_TR_ID = "FHKST76650100";  // TODO: 확인 필요

	// plan.md 기준: ka90007 또는 ka90010 (종목별 프로그램매매 추이)
	// TODO: 키움 Open API 포털에서 경로 및 tr_id 확인 후 수정 필요
	private static final String HISTORY_API_PATH = "/api/dostk/stkinfo";
	private static final String HISTORY_TR_ID_INTRADAY = "ka90007";   // 당일 장중
	private static final String HISTORY_TR_ID_DAILY = "ka90010";      // 일별

	private final KiwoomApiClient kiwoomApiClient;
	private final ProgramTradingRankingSnapshotRepository rankingRepository;
	private final ProgramTradingHistoryRepository historyRepository;
	private final WatchStockRepository watchStockRepository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		collectRankingSnapshots(snapshotTime);
		collectHistoryForWatchStocks(snapshotTime);
	}

	private void collectRankingSnapshots(LocalDateTime snapshotTime) {
		for (ProgramRankingType rankingType : ProgramRankingType.values()) {
			try {
				collectRankingForType(rankingType, snapshotTime);
			} catch (Exception e) {
				log.error("프로그램매매 랭킹 수집 실패: rankingType={}", rankingType, e);
			}
		}
	}

	private void collectRankingForType(ProgramRankingType rankingType, LocalDateTime snapshotTime) {
		List<ProgramTradingRankingSnapshot> existing = rankingRepository
			.findBySnapshotTimeAndRankingTypeOrderByRankAsc(snapshotTime, rankingType);
		if (!existing.isEmpty()) {
			log.debug("프로그램매매 랭킹 이미 존재, 스킵: rankingType={}, snapshotTime={}", rankingType, snapshotTime);
			return;
		}

		// TODO: 정렬 구분 파라미터 값 확인 필요 (순매수: "1", 순매도: "2" 등)
		String sortCode = rankingType == ProgramRankingType.NET_BUY ? "1" : "2";

		JsonNode response = kiwoomApiClient.post(
			RANKING_API_PATH,
			RANKING_TR_ID,
			Map.of("sort_type", sortCode)  // TODO: 파라미터명 확인 필요
		);

		// TODO: 응답 배열 필드명 확인 필요
		JsonNode outputList = response.path("output");
		int rank = 1;
		for (JsonNode item : outputList) {
			// TODO: 아래 필드명들은 키움 API 응답 문서 기준으로 수정 필요
			String stockCode = KiwoomResponseParser.parseString(item, "stck_shrn_iscd");
			String stockName = KiwoomResponseParser.parseString(item, "hts_kor_isnm");
			BigDecimal buyAmount = KiwoomResponseParser.parseBigDecimal(item, "pgmm_cntg_buyqty");
			BigDecimal sellAmount = KiwoomResponseParser.parseBigDecimal(item, "pgmm_cntg_seln_qty");
			BigDecimal netBuyAmount = KiwoomResponseParser.parseBigDecimal(item, "pgmm_cntg_ntby_qty");

			rankingRepository.save(ProgramTradingRankingSnapshot.create(
				rankingType, rank++, stockCode, stockName, buyAmount, sellAmount, netBuyAmount, snapshotTime
			));
		}

		log.debug("프로그램매매 랭킹 수집 완료: rankingType={}, count={}", rankingType, rank - 1);
	}

	private void collectHistoryForWatchStocks(LocalDateTime snapshotTime) {
		List<String> stockCodes = watchStockRepository.findDistinctStockCodes();
		for (String stockCode : stockCodes) {
			try {
				collectHistoryForStock(stockCode, snapshotTime);
			} catch (Exception e) {
				log.error("프로그램매매 이력 수집 실패: stockCode={}", stockCode, e);
			}
		}
	}

	private void collectHistoryForStock(String stockCode, LocalDateTime snapshotTime) {
		JsonNode response = kiwoomApiClient.post(
			HISTORY_API_PATH,
			HISTORY_TR_ID_INTRADAY,
			Map.of("stck_shrn_iscd", stockCode)  // TODO: 파라미터명 확인 필요
		);

		// TODO: 응답 구조 확인 필요. 단일 시점 데이터면 output, 목록이면 output1 등
		JsonNode output = response.path("output");
		// 목록 응답인 경우 최신 항목만 저장
		JsonNode item = output.isArray() ? output.path(0) : output;
		if (item.isMissingNode()) {
			return;
		}

		// TODO: 아래 필드명들은 키움 API 응답 문서 기준으로 수정 필요
		BigDecimal buyAmount = KiwoomResponseParser.parseBigDecimal(item, "pgmm_buy_amt");
		BigDecimal sellAmount = KiwoomResponseParser.parseBigDecimal(item, "pgmm_seln_amt");
		BigDecimal netBuyAmount = KiwoomResponseParser.parseBigDecimal(item, "pgmm_ntby_amt");

		historyRepository.save(ProgramTradingHistory.create(
			stockCode, snapshotTime, buyAmount, sellAmount, netBuyAmount
		));

		log.debug("프로그램매매 이력 수집 완료: stockCode={}", stockCode);
	}
}
