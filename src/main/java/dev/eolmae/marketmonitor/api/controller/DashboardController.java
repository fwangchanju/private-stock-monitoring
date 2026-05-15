package dev.eolmae.marketmonitor.api.controller;

import dev.eolmae.marketmonitor.api.dto.DashboardResponse;
import dev.eolmae.marketmonitor.api.dto.IndexContributionItem;
import dev.eolmae.marketmonitor.api.dto.IntradayInvestorRankingItem;
import dev.eolmae.marketmonitor.api.dto.NotificationSettingResponse;
import dev.eolmae.marketmonitor.api.dto.ProgramTradingDailyHistoryItem;
import dev.eolmae.marketmonitor.api.dto.ProgramTradingHistoryItem;
import dev.eolmae.marketmonitor.api.dto.ProgramTradingRankingItem;
import dev.eolmae.marketmonitor.api.dto.ShortSellingHistoryItem;
import dev.eolmae.marketmonitor.api.dto.SnapshotResponse;
import dev.eolmae.marketmonitor.api.dto.StockHistoryResponse;
import dev.eolmae.marketmonitor.api.dto.WatchStockItem;
import dev.eolmae.marketmonitor.common.enums.AmtQtyType;
import dev.eolmae.marketmonitor.common.enums.IntradayRankingType;
import dev.eolmae.marketmonitor.common.enums.InvestorType;
import dev.eolmae.marketmonitor.common.enums.MarketType;
import dev.eolmae.marketmonitor.common.enums.ProgramRankingType;
import dev.eolmae.marketmonitor.api.service.DashboardQueryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

	private final DashboardQueryService queryService;

	public DashboardController(DashboardQueryService queryService) {
		this.queryService = queryService;
	}

	// ── 메인 대시보드 ─────────────────────────────────────────────────────

	@GetMapping("/dashboard")
	public DashboardResponse getDashboard() {
		return queryService.getDashboard();
	}

	// renderer 준비 후 주석 해제: @PostMapping("/send-dashboard"), SendDashboardResponse, DashboardSendService, BusinessException, Optional 임포트
	// @PostMapping("/send-dashboard")
	// public SendDashboardResponse sendDashboard() {
	//     DashboardSendService svc = dashboardSendService
	//         .orElseThrow(() -> new BusinessException("renderer가 비활성화 상태입니다."));
	//     return new SendDashboardResponse(svc.sendDashboard());
	// }

	// ── 장중 투자자별 매매 상위 상세 ─────────────────────────────────────────

	@GetMapping("/intraday-rankings")
	public SnapshotResponse<IntradayInvestorRankingItem> getIntradayRankings(
		@RequestParam MarketType market,
		@RequestParam InvestorType investor,
		@RequestParam IntradayRankingType ranking
	) {
		return queryService.getIntradayRankings(market, investor, ranking);
	}

	// ── 프로그램매매 상위 상세 ────────────────────────────────────────────

	@GetMapping("/program-trading-rankings")
	public SnapshotResponse<ProgramTradingRankingItem> getProgramTradingRankings(
		@RequestParam ProgramRankingType ranking,
		@RequestParam(required = false) MarketType market,
		@RequestParam(required = false) AmtQtyType amtQty
	) {
		AmtQtyType resolvedAmtQty = amtQty != null ? amtQty : AmtQtyType.AMOUNT;
		return queryService.getProgramTradingRankings(market, ranking, resolvedAmtQty);
	}

	// ── 지수 기여도 상위 상세 ─────────────────────────────────────────────

	@GetMapping("/index-contribution")
	public SnapshotResponse<IndexContributionItem> getIndexContribution(
		@RequestParam MarketType market
	) {
		return queryService.getIndexContribution(market);
	}

	// ── 관심종목 ─────────────────────────────────────────────────────────

	@GetMapping("/watch-stocks")
	public List<WatchStockItem> getWatchStocks() {
		return queryService.getWatchStocks();
	}

	// ── 사용자 설정 ───────────────────────────────────────────────────────

	@GetMapping("/user-settings/default")
	public NotificationSettingResponse getDefaultUserSetting() {
		return queryService.getNotificationSetting();
	}

	// ── 종목별 이력 ───────────────────────────────────────────────────────

	@GetMapping("/stocks/{stockCode}/program-trading")
	public StockHistoryResponse<ProgramTradingHistoryItem> getProgramTradingHistory(
		@PathVariable String stockCode,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
	) {
		return queryService.getProgramTradingHistory(stockCode, from, to);
	}

	@GetMapping("/stocks/{stockCode}/program-trading/daily")
	public StockHistoryResponse<ProgramTradingDailyHistoryItem> getProgramTradingDailyHistory(
		@PathVariable String stockCode,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return queryService.getProgramTradingDailyHistory(stockCode, from, to);
	}

	@GetMapping("/stocks/{stockCode}/short-selling")
	public StockHistoryResponse<ShortSellingHistoryItem> getShortSellingHistory(
		@PathVariable String stockCode,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return queryService.getShortSellingHistory(stockCode, from, to);
	}
}
