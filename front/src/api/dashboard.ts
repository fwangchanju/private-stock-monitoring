import client from './client'
import type {
  DashboardResponse,
  IntradayInvestorRankingItem,
  IntradayRankingType,
  InvestorType,
  IndexContributionItem,
  MarketType,
  ProgramRankingType,
  ProgramTradingHistoryItem,
  ProgramTradingRankingItem,
  ShortSellingHistoryItem,
  SnapshotResponse,
  StockHistoryResponse,
  WatchStockItem,
} from '../types/api'

export const getDashboard = () =>
  client.get<DashboardResponse>('/dashboard').then(r => r.data)

export const getWatchStocks = () =>
  client.get<WatchStockItem[]>('/watch-stocks').then(r => r.data)

export const getIntradayRankings = (
  market: MarketType,
  investor: InvestorType,
  ranking: IntradayRankingType,
) =>
  client
    .get<SnapshotResponse<IntradayInvestorRankingItem>>('/intraday-rankings', {
      params: { market, investor, ranking },
    })
    .then(r => r.data)

export const getProgramTradingRankings = (ranking: ProgramRankingType) =>
  client
    .get<SnapshotResponse<ProgramTradingRankingItem>>('/program-trading-rankings', {
      params: { ranking },
    })
    .then(r => r.data)

export const getIndexContribution = (market: MarketType) =>
  client
    .get<SnapshotResponse<IndexContributionItem>>('/index-contribution', {
      params: { market },
    })
    .then(r => r.data)

export const getProgramTradingHistory = (
  stockCode: string,
  from: string,
  to: string,
) =>
  client
    .get<StockHistoryResponse<ProgramTradingHistoryItem>>(
      `/stocks/${stockCode}/program-trading`,
      { params: { from, to } },
    )
    .then(r => r.data)

export const sendDashboard = () =>
  client.post<{ sent: number }>('/send-dashboard').then(r => r.data)

export const getShortSellingHistory = (
  stockCode: string,
  from: string,
  to: string,
) =>
  client
    .get<StockHistoryResponse<ShortSellingHistoryItem>>(
      `/stocks/${stockCode}/short-selling`,
      { params: { from, to } },
    )
    .then(r => r.data)
