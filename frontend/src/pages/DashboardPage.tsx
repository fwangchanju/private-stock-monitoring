import { useEffect, useState } from 'react'
import { getDashboard } from '../api/dashboard'
import type { DashboardResponse } from '../types/api'
import { toDateTimeLabel } from '../utils/format'
import MarketOverviewSection from '../components/MarketOverviewSection'
import InvestorTradingSection from '../components/InvestorTradingSection'
import IntradayRankingSection from '../components/IntradayRankingSection'
import ProgramTradingSection from '../components/ProgramTradingSection'
import IndexContributionSection from '../components/IndexContributionSection'
import WatchStockSection from '../components/WatchStockSection'

export default function DashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getDashboard()
      .then(setData)
      .catch(() => setError('데이터를 불러오지 못했습니다'))
  }, [])

  if (error) return <div className="loading">{error}</div>
  if (!data) return <div className="loading">불러오는 중...</div>

  return (
    <>
      <header className="app-header">
        <h1>PSMS 대시보드</h1>
        <div className="meta">
          {data.snapshotTime && (
            <span>기준: {toDateTimeLabel(data.snapshotTime)}</span>
          )}
          {data.lastCollectedAt && (
            <span>수집: {toDateTimeLabel(data.lastCollectedAt)}</span>
          )}
          {data.marketStatus && (
            <span>{data.marketStatus}</span>
          )}
        </div>
      </header>
      <div className="page">
        <MarketOverviewSection items={data.marketOverviews} />
        <InvestorTradingSection items={data.investorTradingSummaries} />
        <IntradayRankingSection items={data.intradayTopRankings} />
        <ProgramTradingSection items={data.programTradingHighlights} />
        <IndexContributionSection items={data.indexContributionHighlights} />
        <WatchStockSection items={data.watchStocks} />
      </div>
    </>
  )
}
