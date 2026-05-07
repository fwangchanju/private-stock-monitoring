import { BrowserRouter, Routes, Route } from 'react-router-dom'
import DashboardPage from './pages/DashboardPage'
import IntradayRankingPage from './pages/IntradayRankingPage'
import ProgramTradingPage from './pages/ProgramTradingPage'
import IndexContributionPage from './pages/IndexContributionPage'
import StockDetailPage from './pages/StockDetailPage'
import PrototypeDosPage from './pages/PrototypeDosPage'
import Prototype98Page from './pages/Prototype98Page'
import PrototypeNesPage from './pages/PrototypeNesPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/intraday-rankings" element={<IntradayRankingPage />} />
        <Route path="/program-trading" element={<ProgramTradingPage />} />
        <Route path="/index-contribution" element={<IndexContributionPage />} />
        <Route path="/stocks/:stockCode" element={<StockDetailPage />} />
        <Route path="/a" element={<PrototypeDosPage />} />
        <Route path="/b" element={<Prototype98Page />} />
        <Route path="/c" element={<PrototypeNesPage />} />
      </Routes>
    </BrowserRouter>
  )
}
