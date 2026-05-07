import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { IndexContributionItem, MarketType } from '../types/api'
import { toPctSigned, signClass } from '../utils/format'

interface Props {
  items: IndexContributionItem[]
}

const MARKETS: MarketType[] = ['KOSPI', 'KOSDAQ']

export default function IndexContributionSection({ items }: Props) {
  const [market, setMarket] = useState<MarketType>('KOSPI')

  const filtered = items
    .filter(i => i.marketType === market)
    .sort((a, b) => a.rank - b.rank)
    .slice(0, 10)

  return (
    <section className="section">
      <div className="section-header">
        <h2>지수 기여도 상위</h2>
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
          <Link
            to={`/index-contribution?market=${market}`}
            style={{ fontSize: 12, color: 'var(--text-muted)' }}
          >
            전체 보기 →
          </Link>
        </div>
      </div>

      {items.length === 0 ? (
        <div className="empty-state">수집된 데이터가 없습니다</div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">{market} 데이터 없음</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th className="left" style={{ width: 32 }}>#</th>
              <th className="left">종목</th>
              <th>기여도</th>
              <th>등락률</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(item => (
              <tr key={`${item.rank}-${item.stockCode}`}>
                <td className="left" style={{ color: 'var(--text-muted)', fontSize: 11 }}>
                  {item.rank}
                </td>
                <td className="left">
                  <Link to={`/stocks/${item.stockCode}`}>{item.stockName}</Link>
                  <span style={{ marginLeft: 6, fontSize: 11, color: 'var(--text-muted)' }}>
                    {item.stockCode}
                  </span>
                </td>
                <td className={signClass(item.contributionScore)}>
                  {item.contributionScore.toFixed(2)}
                </td>
                <td className={signClass(item.priceChangeRate)}>
                  {toPctSigned(item.priceChangeRate)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
