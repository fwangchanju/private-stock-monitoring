package dev.eolmae.psms.api;

import dev.eolmae.psms.api.dto.DashboardResponse;
import dev.eolmae.psms.api.dto.IndexContributionItem;
import dev.eolmae.psms.api.dto.IntradayInvestorRankingItem;
import dev.eolmae.psms.api.dto.InvestorTradingSummaryItem;
import dev.eolmae.psms.api.dto.MarketOverviewItem;
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
import dev.eolmae.psms.domain.dashboard.IndexContributionRankingSnapshotRepository;
import dev.eolmae.psms.domain.dashboard.IntradayInvestorRankingSnapshotRepository;
import dev.eolmae.psms.domain.dashboard.InvestorTradingSummaryRepository;
import dev.eolmae.psms.domain.dashboard.MarketOverview;
import dev.eolmae.psms.domain.dashboard.MarketOverviewRepository;
import dev.eolmae.psms.domain.dashboard.ProgramTradingRankingSnapshot;
import dev.eolmae.psms.domain.dashboard.ProgramTradingRankingSnapshotRepository;
import dev.eolmae.psms.domain.history.ProgramTradingDailyHistoryRepository;
import dev.eolmae.psms.domain.history.ProgramTradingHistoryRepository;
import dev.eolmae.psms.domain.history.ShortSellingHistoryRepository;
import dev.eolmae.psms.domain.stock.WatchStockRepository;
import dev.eolmae.psms.domain.user.UserNotificationSettingRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
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
	private final ProgramTradingDailyHistoryRepository programTradingDailyHistoryRepository;
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
		ProgramTradingDailyHistoryRepository programTradingDailyHistoryRepository,
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
		this.programTradingDailyHistoryRepository = programTradingDailyHistoryRepository;
		this.shortSellingHistoryRepository = shortSellingHistoryRepository;
	}

	public DashboardResponse getDashboard() {
		var marketOverviews = marketOverviewRepository.findAllByOrderByMarketTypeAsc();
		var investorSummaries = investorTradingSummaryRepository.findAllByOrderByMarketTypeAscInvestorTypeAsc();

		// 아직 수집된 데이터가 없으면 빈 응답 반환
		if (marketOverviews.isEmpty()) {
			return new DashboardResponse(
				null, null, null,
				List.of(), List.of(), List.of(), List.of(), List.of(),
				getWatchStocks(), getNotificationSetting()
			);
		}

		var snapshotTime = marketOverviews.stream()
			.map(MarketOverview::getSnapshotTime)
			.max(LocalDateTime::compareTo)
			.orElseThrow();

		var lastCollectedAt = marketOverviews.stream()
			.map(MarketOverview::getLastCollectedAt)
			.max(LocalDateTime::compareTo)
			.orElseThrow();

		var marketStatus = marketOverviews.stream()
			.max(Comparator.comparing(overview -> overview.getChangeRate().abs()))
			.map(MarketOverview::getMarketStatus)
			.orElse("UNKNOWN");

		// 대시보드 요약: KOSPI 기준 외국인 순매수 상위 (대표 조합)
		var intradayItems = intradayInvestorRankingSnapshotRepository
			.findBySnapshotTimeAndMarketTypeAndInvestorTypeAndRankingTypeOrderByRankAsc(
				snapshotTime, MarketType.KOSPI, InvestorType.FOREIGNER, IntradayRankingType.NET_BUY)
			.stream().map(item -> new IntradayInvestorRankingItem(
				item.getMarketType(), item.getInvestorType(), item.getRank(),
				item.getStockCode(), item.getStockName(),
				item.getNetBuyAmount(), item.getTradedVolume()
			)).toList();

		// 대시보드 요약: 프로그램 순매수 상위 (KOSPI 기준 금액)
		var programItems = getProgramTradingRankings(MarketType.KOSPI, ProgramRankingType.NET_BUY, AmtQtyType.AMOUNT).items();

		// 대시보드 요약: KOSPI 지수 기여도 상위
		var indexContributionItems = indexContributionRankingSnapshotRepository
			.findBySnapshotTimeAndMarketTypeOrderByRankAsc(snapshotTime, MarketType.KOSPI)
			.stream().map(item -> new IndexContributionItem(
				item.getMarketType(), item.getRank(), item.getStockCode(), item.getStockName(),
				item.getContributionScore(), item.getPriceChangeRate()
			)).toList();

		return new DashboardResponse(
			snapshotTime,
			lastCollectedAt,
			marketStatus,
			marketOverviews.stream().map(item -> new MarketOverviewItem(
				item.getMarketType(), item.getMarketStatus(),
				item.getIndexValue(), item.getChangeValue(), item.getChangeRate(),
				item.getTradingValue(), item.getUpperLimitCount(), item.getLowerLimitCount(),
				item.getAdvancers(), item.getDecliners(), item.getUnchangedCount()
			)).toList(),
			investorSummaries.stream().map(item -> new InvestorTradingSummaryItem(
				item.getMarketType(), item.getInvestorType(),
				item.getBuyAmount(), item.getSellAmount(), item.getNetBuyAmount()
			)).toList(),
			intradayItems,
			programItems,
			indexContributionItems,
			getWatchStocks(),
			getNotificationSetting()
		);
	}

	public SnapshotResponse<IntradayInvestorRankingItem> getIntradayRankings(
		MarketType marketType, InvestorType investorType, IntradayRankingType rankingType
	) {
		var snapshotTime = intradayInvestorRankingSnapshotRepository.findLatestSnapshotTime()
			.orElse(null);
		if (snapshotTime == null) {
			return new SnapshotResponse<>(null, List.of());
		}

		var items = intradayInvestorRankingSnapshotRepository
			.findBySnapshotTimeAndMarketTypeAndInvestorTypeAndRankingTypeOrderByRankAsc(
				snapshotTime, marketType, investorType, rankingType)
			.stream().map(item -> new IntradayInvestorRankingItem(
				item.getMarketType(), item.getInvestorType(), item.getRank(),
				item.getStockCode(), item.getStockName(),
				item.getNetBuyAmount(), item.getTradedVolume()
			)).toList();

		return new SnapshotResponse<>(snapshotTime, items);
	}

	public SnapshotResponse<ProgramTradingRankingItem> getProgramTradingRankings(
		MarketType marketType, ProgramRankingType rankingType, AmtQtyType amtQtyType
	) {
		var snapshotTime = programTradingRankingSnapshotRepository.findLatestSnapshotTime()
			.orElse(null);
		if (snapshotTime == null) {
			return new SnapshotResponse<>(null, List.of());
		}

		List<ProgramTradingRankingSnapshot> snapshots;
		if (marketType == null) {
			// 전체: KOSPI + KOSDAQ 합산 후 netBuyAmount 기준 재정렬
			var kospi = programTradingRankingSnapshotRepository
				.findBySnapshotTimeAndMarketTypeAndRankingTypeAndAmtQtyTypeOrderByRankAsc(
					snapshotTime, MarketType.KOSPI, rankingType, amtQtyType);
			var kosdaq = programTradingRankingSnapshotRepository
				.findBySnapshotTimeAndMarketTypeAndRankingTypeAndAmtQtyTypeOrderByRankAsc(
					snapshotTime, MarketType.KOSDAQ, rankingType, amtQtyType);
			snapshots = Stream.concat(kospi.stream(), kosdaq.stream())
				.sorted(Comparator.comparing(ProgramTradingRankingSnapshot::getProgramNetBuyAmount).reversed())
				.toList();
		} else {
			snapshots = programTradingRankingSnapshotRepository
				.findBySnapshotTimeAndMarketTypeAndRankingTypeAndAmtQtyTypeOrderByRankAsc(
					snapshotTime, marketType, rankingType, amtQtyType);
		}

		return new SnapshotResponse<>(snapshotTime, snapshots.stream()
			.map(item -> new ProgramTradingRankingItem(
				item.getRank(), item.getStockCode(), item.getStockName(),
				item.getProgramBuyAmount(), item.getProgramSellAmount(), item.getProgramNetBuyAmount()
			)).toList());
	}

	public SnapshotResponse<IndexContributionItem> getIndexContribution(MarketType marketType) {
		var snapshotTime = indexContributionRankingSnapshotRepository.findLatestSnapshotTime()
			.orElse(null);
		if (snapshotTime == null) {
			return new SnapshotResponse<>(null, List.of());
		}

		var items = indexContributionRankingSnapshotRepository
			.findBySnapshotTimeAndMarketTypeOrderByRankAsc(snapshotTime, marketType)
			.stream().map(item -> new IndexContributionItem(
				item.getMarketType(), item.getRank(), item.getStockCode(), item.getStockName(),
				item.getContributionScore(), item.getPriceChangeRate()
			)).toList();

		return new SnapshotResponse<>(snapshotTime, items);
	}

	public NotificationSettingResponse getNotificationSetting() {
		return userNotificationSettingRepository.findByUserUserKey(DEFAULT_USER_KEY)
			.map(setting -> new NotificationSettingResponse(
				setting.getUser().getUserKey(),
				setting.isReminderEnabled(),
				setting.getReminderTime(),
				setting.getTimezone()
			))
			.orElse(null);
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

	public StockHistoryResponse<ProgramTradingHistoryItem> getProgramTradingHistory(
		String stockCode, LocalDateTime from, LocalDateTime to
	) {
		var items = programTradingHistoryRepository
			.findByStockCodeAndSnapshotTimeBetweenOrderBySnapshotTimeAsc(stockCode, from, to)
			.stream().map(item -> new ProgramTradingHistoryItem(
				item.getSnapshotTime(),
				item.getProgramBuyAmount(),
				item.getProgramSellAmount(),
				item.getProgramNetBuyAmount()
			)).toList();

		return new StockHistoryResponse<>(stockCode, items);
	}

	public StockHistoryResponse<ProgramTradingDailyHistoryItem> getProgramTradingDailyHistory(
		String stockCode, LocalDate from, LocalDate to
	) {
		var items = programTradingDailyHistoryRepository
			.findByStockCodeAndTradeDateBetweenOrderByTradeDateDesc(stockCode, from, to)
			.stream().map(item -> new ProgramTradingDailyHistoryItem(
				item.getTradeDate(),
				item.getProgramBuyAmount(),
				item.getProgramSellAmount(),
				item.getProgramNetBuyAmount()
			)).toList();

		return new StockHistoryResponse<>(stockCode, items);
	}

	public StockHistoryResponse<ShortSellingHistoryItem> getShortSellingHistory(
		String stockCode, LocalDate from, LocalDate to
	) {
		var items = shortSellingHistoryRepository
			.findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(stockCode, from, to)
			.stream().map(item -> new ShortSellingHistoryItem(
				item.getTradeDate(),
				item.getShortVolume(),
				item.getShortBalanceVolume(),
				item.getShortAmount(),
				item.getShortAvgPrice(),
				item.getShortRatio(),
				item.getClosePrice(),
				item.getPriceChange(),
				item.getChangeRate()
			)).toList();

		return new StockHistoryResponse<>(stockCode, items);
	}
}
