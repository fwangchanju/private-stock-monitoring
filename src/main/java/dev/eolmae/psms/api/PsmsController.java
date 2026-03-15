package dev.eolmae.psms.api;

import dev.eolmae.psms.api.dto.DashboardResponse;
import dev.eolmae.psms.api.dto.NotificationSettingResponse;
import dev.eolmae.psms.api.dto.ProgramTradingHistoryItem;
import dev.eolmae.psms.api.dto.ShortSellingHistoryItem;
import dev.eolmae.psms.api.dto.StockHistoryResponse;
import dev.eolmae.psms.api.dto.WatchStockItem;
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
public class PsmsController {

	private final PsmsQueryService psmsQueryService;

	public PsmsController(PsmsQueryService psmsQueryService) {
		this.psmsQueryService = psmsQueryService;
	}

	@GetMapping("/dashboard")
	public DashboardResponse getDashboard() {
		return psmsQueryService.getDashboard();
	}

	@GetMapping("/watch-stocks")
	public List<WatchStockItem> getWatchStocks() {
		return psmsQueryService.getWatchStocks();
	}

	@GetMapping("/user-settings/default")
	public NotificationSettingResponse getDefaultUserSetting() {
		return psmsQueryService.getNotificationSetting();
	}

	@GetMapping("/stocks/{stockCode}/program-trading")
	public StockHistoryResponse<ProgramTradingHistoryItem> getProgramTradingHistory(
		@PathVariable String stockCode,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
	) {
		return psmsQueryService.getProgramTradingHistory(stockCode, from, to);
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
