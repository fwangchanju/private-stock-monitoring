import { BrowserRouter, Routes, Route } from 'react-router-dom'
import DashboardPage from './pages/DashboardPage'
import IntradayRankingPage from './pages/IntradayRankingPage'
import ProgramTradingPage from './pages/ProgramTradingPage'
import IndexContributionPage from './pages/IndexContributionPage'
import StockDetailPage from './pages/StockDetailPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/intraday-rankings" element={<IntradayRankingPage />} />
        <Route path="/program-trading" element={<ProgramTradingPage />} />
        <Route path="/index-contribution" element={<IndexContributionPage />} />
        <Route path="/stocks/:stockCode" element={<StockDetailPage />} />
      </Routes>
    </BrowserRouter>
  )
}
