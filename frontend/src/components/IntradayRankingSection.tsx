import { useState } from 'react'
import { Link } from 'react-router-dom'
import type {
  IntradayInvestorRankingItem,
  InvestorType,
  MarketType,
  IntradayRankingType,
} from '../types/api'
import { toEokSigned, toVolume, signClass, investorLabel, marketLabel } from '../utils/format'

interface Props {
  items: IntradayInvestorRankingItem[]
}

const INVESTORS: InvestorType[] = ['PERSONAL', 'FOREIGNER', 'INSTITUTION']
const MARKETS: MarketType[] = ['KOSPI', 'KOSDAQ']
const RANKINGS: IntradayRankingType[] = ['NET_BUY', 'NET_SELL']

export default function IntradayRankingSection({ items }: Props) {
  const [market, setMarket] = useState<MarketType>('KOSPI')
  const [investor, setInvestor] = useState<InvestorType>('FOREIGNER')
  const [ranking, setRanking] = useState<IntradayRankingType>('NET_BUY')

  const filtered = items.filter(
    i => i.marketType === market && i.investorType === investor && i.rank <= 10,
  )

  return (
    <section className="section">
      <div className="section-header">
        <h2>장중 투자자별 매매 상위</h2>
        <div className="actions">
          <div className="tab-bar">
            {MARKETS.map(m => (
              <button
                key={m}
                className={`tab-btn ${market === m ? 'active' : ''}`}
                onClick={() => setMarket(m)}
              >
                {m}
              </button>
            ))}
          </div>
          <div className="tab-bar">
            {INVESTORS.map(inv => (
              <button
                key={inv}
                className={`tab-btn ${investor === inv ? 'active' : ''}`}
                onClick={() => setInvestor(inv)}
              >
                {investorLabel(inv)}
              </button>
            ))}
          </div>
          <div className="tab-bar">
            {RANKINGS.map(r => (
              <button
                key={r}
                className={`tab-btn ${ranking === r ? 'active' : ''}`}
                onClick={() => setRanking(r)}
              >
                {r === 'NET_BUY' ? '순매수' : '순매도'}
              </button>
            ))}
          </div>
          <Link
            to={`/intraday-rankings?market=${market}&investor=${investor}&ranking=${ranking}`}
            style={{ fontSize: 12, color: 'var(--text-muted)' }}
          >
            전체 보기 →
          </Link>
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="empty-state">
          {items.length === 0
            ? '수집된 데이터가 없습니다'
            : `${marketLabel(market)} ${investorLabel(investor)} 데이터 없음`}
        </div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th className="left" style={{ width: 32 }}>#</th>
              <th className="left">종목</th>
              <th>순매수(억)</th>
              <th>거래량</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(item => (
              <tr key={`${item.rank}-${item.stockCode}`}>
                <td className="left" style={{ color: 'var(--text-muted)', fontSize: 11 }}>
                  {item.rank}
                </td>
                <td className="left">
                  <span>{item.stockName}</span>
                  <span style={{ marginLeft: 6, fontSize: 11, color: 'var(--text-muted)' }}>
                    {item.stockCode}
                  </span>
                </td>
                <td className={signClass(item.netBuyAmount)}>
                  {toEokSigned(item.netBuyAmount)}
                </td>
                <td style={{ color: 'var(--text-muted)' }}>
                  {toVolume(item.tradedVolume)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
