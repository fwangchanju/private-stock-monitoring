package dev.eolmae.psms.api;

import dev.eolmae.psms.api.dto.DashboardResponse;
import dev.eolmae.psms.api.dto.IndexContributionItem;
import dev.eolmae.psms.api.dto.IntradayInvestorRankingItem;
import dev.eolmae.psms.api.dto.InvestorTradingSummaryItem;
import dev.eolmae.psms.api.dto.MarketOverviewItem;
import dev.eolmae.psms.api.dto.NotificationSettingResponse;
import dev.eolmae.psms.api.dto.ProgramTradingHistoryItem;
import dev.eolmae.psms.api.dto.ProgramTradingRankingItem;
import dev.eolmae.psms.api.dto.ShortSellingHistoryItem;
import dev.eolmae.psms.api.dto.StockHistoryResponse;
import dev.eolmae.psms.api.dto.WatchStockItem;
import dev.eolmae.psms.domain.common.IntradayRankingType;
import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.common.ProgramRankingType;
import dev.eolmae.psms.domain.dashboard.IndexContributionRankingSnapshotRepository;
import dev.eolmae.psms.domain.dashboard.IntradayInvestorRankingSnapshotRepository;
import dev.eolmae.psms.domain.dashboard.InvestorTradingSummaryRepository;
import dev.eolmae.psms.domain.dashboard.MarketOverviewRepository;
import dev.eolmae.psms.domain.dashboard.ProgramTradingRankingSnapshotRepository;
import dev.eolmae.psms.domain.history.ProgramTradingHistoryRepository;
import dev.eolmae.psms.domain.history.ShortSellingHistoryRepository;
import dev.eolmae.psms.domain.stock.WatchStockRepository;
import dev.eolmae.psms.domain.user.UserNotificationSettingRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PsmsQueryService {

	private static final String DEFAULT_USER_KEY = "default";

	private final MarketOverviewRepository marketOverviewRepository;
	private final InvestorTradingSummaryRepository investorTradingSummaryRepository;
	private final IntradayInvestorRankingSnapshotRepository intradayInvestorRankingSnapshotRepository;
	private final ProgramTradingRankingSnapshotRepository programTradingRankingSnapshotRepository;
	private final IndexContributionRankingSnapshotRepository indexContributionRankingSnapshotRepository;
	private final WatchStockRepository watchStockRepository;
	private final UserNotificationSettingRepository userNotificationSettingRepository;
	private final ProgramTradingHistoryRepository programTradingHistoryRepository;
	private final ShortSellingHistoryRepository shortSellingHistoryRepository;

	public PsmsQueryService(
		MarketOverviewRepository marketOverviewRepository,
		InvestorTradingSummaryRepository investorTradingSummaryRepository,
		IntradayInvestorRankingSnapshotRepository intradayInvestorRankingSnapshotRepository,
		ProgramTradingRankingSnapshotRepository programTradingRankingSnapshotRepository,
		IndexContributionRankingSnapshotRepository indexContributionRankingSnapshotRepository,
		WatchStockRepository watchStockRepository,
		UserNotificationSettingRepository userNotificationSettingRepository,
		ProgramTradingHistoryRepository programTradingHistoryRepository,
		ShortSellingHistoryRepository shortSellingHistoryRepository
	) {
		this.marketOverviewRepository = marketOverviewRepository;
		this.investorTradingSummaryRepository = investorTradingSummaryRepository;
		this.intradayInvestorRankingSnapshotRepository = intradayInvestorRankingSnapshotRepository;
		this.programTradingRankingSnapshotRepository = programTradingRankingSnapshotRepository;
		this.indexContributionRankingSnapshotRepository = indexContributionRankingSnapshotRepository;
		this.watchStockRepository = watchStockRepository;
		this.userNotificationSettingRepository = userNotificationSettingRepository;
		this.programTradingHistoryRepository = programTradingHistoryRepository;
		this.shortSellingHistoryRepository = shortSellingHistoryRepository;
	}

	public DashboardResponse getDashboard() {
		var marketOverviews = marketOverviewRepository.findAllByOrderByMarketTypeAsc();
		var investorSummaries = investorTradingSummaryRepository.findAllByOrderByMarketTypeAscInvestorTypeAsc();
		var snapshotTime = marketOverviews.stream()
			.map(overview -> overview.getSnapshotTime())
			.max(LocalDateTime::compareTo)
			.orElseThrow();
		var lastCollectedAt = marketOverviews.stream()
			.map(overview -> overview.getLastCollectedAt())
			.max(LocalDateTime::compareTo)
			.orElseThrow();
		var marketStatus = marketOverviews.stream()
			.max(Comparator.comparing(overview -> overview.getChangeRate().abs()))
			.map(overview -> overview.getMarketStatus())
			.orElse("UNKNOWN");

		var intradayItems = intradayInvestorRankingSnapshotRepository.findBySnapshotTimeAndMarketTypeAndInvestorTypeAndRankingTypeOrderByRankAsc(
			snapshotTime,
			MarketType.KOSPI,
			InvestorType.FOREIGNER,
			IntradayRankingType.NET_BUY
		).stream().map(item -> new IntradayInvestorRankingItem(
			item.getMarketType(),
			item.getInvestorType(),
			item.getRank(),
			item.getStockCode(),
			item.getStockName(),
			item.getNetBuyAmount(),
			item.getTradedVolume()
		)).toList();

		var programItems = programTradingRankingSnapshotRepository.findBySnapshotTimeAndRankingTypeOrderByRankAsc(
			snapshotTime,
			ProgramRankingType.NET_BUY
		).stream().map(item -> new ProgramTradingRankingItem(
			item.getRank(),
			item.getStockCode(),
			item.getStockName(),
			item.getProgramBuyAmount(),
			item.getProgramSellAmount(),
			item.getProgramNetBuyAmount()
		)).toList();

		var indexContributionItems = indexContributionRankingSnapshotRepository.findBySnapshotTimeAndMarketTypeOrderByRankAsc(
			snapshotTime,
			MarketType.KOSPI
		).stream().map(item -> new IndexContributionItem(
			item.getMarketType(),
			item.getRank(),
			item.getStockCode(),
			item.getStockName(),
			item.getContributionScore(),
			item.getPriceChangeRate()
		)).toList();

		return new DashboardResponse(
			snapshotTime,
			lastCollectedAt,
			marketStatus,
			marketOverviews.stream().map(item -> new MarketOverviewItem(
				item.getMarketType(),
				item.getMarketStatus(),
				item.getIndexValue(),
				item.getChangeValue(),
				item.getChangeRate(),
				item.getTradingValue(),
				item.getAdvancers(),
				item.getDecliners(),
				item.getUnchangedCount()
			)).toList(),
			investorSummaries.stream().map(item -> new InvestorTradingSummaryItem(
				item.getMarketType(),
				item.getInvestorType(),
				item.getBuyAmount(),
				item.getSellAmount(),
				item.getNetBuyAmount()
			)).toList(),
			intradayItems,
			programItems,
			indexContributionItems,
			getWatchStocks(),
			getNotificationSetting()
		);
	}

	public NotificationSettingResponse getNotificationSetting() {
		var setting = userNotificationSettingRepository.findByUserUserKey(DEFAULT_USER_KEY).orElseThrow();
		return new NotificationSettingResponse(
			setting.getUser().getUserKey(),
			setting.isReminderEnabled(),
			setting.getReminderTime(),
			setting.getTimezone()
		);
	}

	public List<WatchStockItem> getWatchStocks() {
		return watchStockRepository.findByUserUserKeyOrderByDisplayOrderAsc(DEFAULT_USER_KEY).stream()
			.map(item -> new WatchStockItem(
				item.getStock().getStockCode(),
				item.getStock().getStockName(),
				item.getStock().getMarketType(),
				item.getDisplayOrder()
			))
			.toList();
	}

	public StockHistoryResponse<ProgramTradingHistoryItem> getProgramTradingHistory(String stockCode, LocalDateTime from, LocalDateTime to) {
		return new StockHistoryResponse<>(
			stockCode,
			programTradingHistoryRepository.findByStockCodeAndSnapshotTimeBetweenOrderBySnapshotTimeAsc(stockCode, from, to).stream()
				.map(item -> new ProgramTradingHistoryItem(
					item.getSnapshotTime(),
					item.getProgramBuyAmount(),
					item.getProgramSellAmount(),
					item.getProgramNetBuyAmount()
				))
				.toList()
		);
	}

	public StockHistoryResponse<ShortSellingHistoryItem> getShortSellingHistory(String stockCode, LocalDate from, LocalDate to) {
		return new StockHistoryResponse<>(
			stockCode,
			shortSellingHistoryRepository.findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(stockCode, from, to).stream()
				.map(item -> new ShortSellingHistoryItem(
					item.getTradeDate(),
					item.getShortVolume(),
					item.getShortAmount(),
					item.getShortRatio()
				))
				.toList()
		);
	}
}
