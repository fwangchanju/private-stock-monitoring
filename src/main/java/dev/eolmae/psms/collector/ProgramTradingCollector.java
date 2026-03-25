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
public class ProgramTradingCollector {

	// ka90003: 프로그램순매수상위50요청 (종목정보 카테고리)
	private static final String RANKING_API_PATH = "/api/dostk/stkinfo";
	private static final String RANKING_TR_ID = "ka90003";

	// ka90008: 종목시간별프로그램매매추이요청 (시세 카테고리)
	private static final String HISTORY_API_PATH = "/api/dostk/mrkcond";
	private static final String HISTORY_TR_ID = "ka90008";
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

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

		// trde_upper_tp: 2=순매수상위, 1=순매도상위
		String trdeUpperTp = rankingType == ProgramRankingType.NET_BUY ? "2" : "1";

		JsonNode response = kiwoomApiClient.post(
			RANKING_API_PATH,
			RANKING_TR_ID,
			Map.of(
				"trde_upper_tp", trdeUpperTp,
				"amt_qty_tp", "1",      // 1=금액
				"mrkt_tp", "P00101",    // P00101=코스피, P10102=코스닥
				"stex_tp", "3"          // 3=통합
			)
		);

		// 응답 배열: prm_netprps_upper_50
		JsonNode outputList = response.path("prm_netprps_upper_50");
		int rank = 1;
		for (JsonNode item : outputList) {
			String stockCode = KiwoomResponseParser.parseString(item, "stk_cd");
			String stockName = KiwoomResponseParser.parseString(item, "stk_nm");
			BigDecimal buyAmount = KiwoomResponseParser.parseBigDecimal(item, "prm_buy_amt");
			BigDecimal sellAmount = KiwoomResponseParser.parseBigDecimal(item, "prm_sell_amt");
			BigDecimal netBuyAmount = KiwoomResponseParser.parseBigDecimal(item, "prm_netprps_amt");

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
		String dateStr = snapshotTime.format(DATE_FMT);
		JsonNode response = kiwoomApiClient.post(
			HISTORY_API_PATH,
			HISTORY_TR_ID,
			Map.of(
				"stk_cd", stockCode,
				"amt_qty_tp", "1",  // 1=금액
				"date", dateStr
			)
		);

		// 응답 배열: stk_tm_prm_trde_trnsn (시간별 프로그램 매매 추이)
		// 가장 최근 시간 항목(첫 번째)만 저장
		JsonNode outputList = response.path("stk_tm_prm_trde_trnsn");
		JsonNode item = outputList.isArray() && outputList.size() > 0 ? outputList.get(0) : null;
		if (item == null) {
			return;
		}

		BigDecimal buyAmount = KiwoomResponseParser.parseBigDecimal(item, "prm_buy_amt");
		BigDecimal sellAmount = KiwoomResponseParser.parseBigDecimal(item, "prm_sell_amt");
		BigDecimal netBuyAmount = KiwoomResponseParser.parseBigDecimal(item, "prm_netprps_amt");

		historyRepository.save(ProgramTradingHistory.create(
			stockCode, snapshotTime, buyAmount, sellAmount, netBuyAmount
		));

		log.debug("프로그램매매 이력 수집 완료: stockCode={}", stockCode);
	}
}
