package dev.eolmae.psms.api;

import dev.eolmae.psms.api.dto.DashboardResponse;
import dev.eolmae.psms.api.dto.IndexContributionItem;
import dev.eolmae.psms.api.dto.IntradayInvestorRankingItem;
import dev.eolmae.psms.api.dto.NotificationSettingResponse;
import dev.eolmae.psms.api.dto.ProgramTradingDailyHistoryItem;
import dev.eolmae.psms.api.dto.ProgramTradingHistoryItem;
import dev.eolmae.psms.api.dto.ProgramTradingRankingItem;
import dev.eolmae.psms.api.dto.ShortSellingHistoryItem;
import dev.eolmae.psms.api.dto.SnapshotResponse;
import dev.eolmae.psms.api.dto.StockHistoryResponse;
import dev.eolmae.psms.api.dto.WatchStockItem;
import dev.eolmae.psms.domain.common.AmtQtyType;
import dev.eolmae.psms.domain.common.IntradayRankingType;
import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.common.ProgramRankingType;
import dev.eolmae.psms.notification.DashboardSendService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PsmsController {

	private final PsmsQueryService psmsQueryService;
	private final Optional<DashboardSendService> dashboardSendService;

	public PsmsController(PsmsQueryService psmsQueryService, Optional<DashboardSendService> dashboardSendService) {
		this.psmsQueryService = psmsQueryService;
		this.dashboardSendService = dashboardSendService;
	}

	// ── 메인 대시보드 ─────────────────────────────────────────────────────

	@GetMapping("/dashboard")
	public DashboardResponse getDashboard() {
		return psmsQueryService.getDashboard();
	}

	@PostMapping("/send-dashboard")
	public Map<String, Integer> sendDashboard() {
		int sent = dashboardSendService.map(DashboardSendService::sendDashboard).orElse(0);
		return Map.of("sent", sent);
	}

	// ── 장중 투자자별 매매 상위 상세 ─────────────────────────────────────────

	@GetMapping("/intraday-rankings")
	public SnapshotResponse<IntradayInvestorRankingItem> getIntradayRankings(
		@RequestParam MarketType market,
		@RequestParam InvestorType investor,
		@RequestParam IntradayRankingType ranking
	) {
		return psmsQueryService.getIntradayRankings(market, investor, ranking);
	}

	// ── 프로그램매매 상위 상세 ────────────────────────────────────────────

	@GetMapping("/program-trading-rankings")
	public SnapshotResponse<ProgramTradingRankingItem> getProgramTradingRankings(
		@RequestParam ProgramRankingType ranking,
		@RequestParam(required = false) MarketType market,
		@RequestParam(required = false) AmtQtyType amtQty
	) {
		AmtQtyType resolvedAmtQty = amtQty != null ? amtQty : AmtQtyType.AMOUNT;
		return psmsQueryService.getProgramTradingRankings(market, ranking, resolvedAmtQty);
	}

	// ── 지수 기여도 상위 상세 ─────────────────────────────────────────────

	@GetMapping("/index-contribution")
	public SnapshotResponse<IndexContributionItem> getIndexContribution(
		@RequestParam MarketType market
	) {
		return psmsQueryService.getIndexContribution(market);
	}

	// ── 관심종목 ─────────────────────────────────────────────────────────

	@GetMapping("/watch-stocks")
	public List<WatchStockItem> getWatchStocks() {
		return psmsQueryService.getWatchStocks();
	}

	// ── 사용자 설정 ───────────────────────────────────────────────────────

	@GetMapping("/user-settings/default")
	public NotificationSettingResponse getDefaultUserSetting() {
		return psmsQueryService.getNotificationSetting();
	}

	// ── 종목별 이력 ───────────────────────────────────────────────────────

	@GetMapping("/stocks/{stockCode}/program-trading")
	public StockHistoryResponse<ProgramTradingHistoryItem> getProgramTradingHistory(
		@PathVariable String stockCode,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
	) {
		return psmsQueryService.getProgramTradingHistory(stockCode, from, to);
	}

	@GetMapping("/stocks/{stockCode}/program-trading/daily")
	public StockHistoryResponse<ProgramTradingDailyHistoryItem> getProgramTradingDailyHistory(
		@PathVariable String stockCode,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return psmsQueryService.getProgramTradingDailyHistory(stockCode, from, to);
	}

	@GetMapping("/stocks/{stockCode}/short-selling")
	public StockHistoryResponse<ShortSellingHistoryItem> getShortSellingHistory(
		@PathVariable String stockCode,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return psmsQueryService.getShortSellingHistory(stockCode, from, to);
	}
}
