// ─── Enums ───────────────────────────────────────────────────────────────────

export type MarketType = 'KOSPI' | 'KOSDAQ'
export type InvestorType = 'PERSONAL' | 'FOREIGNER' | 'INSTITUTION'
export type IntradayRankingType = 'NET_BUY' | 'NET_SELL'
export type ProgramRankingType = 'NET_BUY' | 'NET_SELL'

// ─── Dashboard ───────────────────────────────────────────────────────────────

export interface MarketOverviewItem {
  marketType: MarketType
  marketStatus: string
  indexValue: number
  changeValue: number
  changeRate: number
  tradingValue: number
  advancers: number
  decliners: number
  unchangedCount: number
}

export interface InvestorTradingSummaryItem {
  marketType: MarketType
  investorType: InvestorType
  buyAmount: number
  sellAmount: number
  netBuyAmount: number
}

export interface IntradayInvestorRankingItem {
  marketType: MarketType
  investorType: InvestorType
  rank: number
  stockCode: string
  stockName: string
  netBuyAmount: number
  tradedVolume: number
}

export interface ProgramTradingRankingItem {
  rank: number
  stockCode: string
  stockName: string
  programBuyAmount: number
  programSellAmount: number
  programNetBuyAmount: number
}

export interface IndexContributionItem {
  marketType: MarketType
  rank: number
  stockCode: string
  stockName: string
  contributionScore: number
  priceChangeRate: number
}

export interface WatchStockItem {
  stockCode: string
  stockName: string
  marketType: MarketType
  displayOrder: number
}

export interface NotificationSettingResponse {
  userKey: string
  reminderEnabled: boolean
  reminderTime: string
  timezone: string
}

export interface DashboardResponse {
  snapshotTime: string | null
  lastCollectedAt: string | null
  marketStatus: string | null
  marketOverviews: MarketOverviewItem[]
  investorTradingSummaries: InvestorTradingSummaryItem[]
  intradayTopRankings: IntradayInvestorRankingItem[]
  programTradingHighlights: ProgramTradingRankingItem[]
  indexContributionHighlights: IndexContributionItem[]
  watchStocks: WatchStockItem[]
  notificationSetting: NotificationSettingResponse | null
}

// ─── Detail ──────────────────────────────────────────────────────────────────

export interface SnapshotResponse<T> {
  snapshotTime: string | null
  items: T[]
}

export interface ProgramTradingHistoryItem {
  snapshotTime: string
  programBuyAmount: number
  programSellAmount: number
  programNetBuyAmount: number
}

export interface ShortSellingHistoryItem {
  tradeDate: string
  shortVolume: number
  shortAmount: number
  shortRatio: number
}

export interface StockHistoryResponse<T> {
  stockCode: string
  items: T[]
}
