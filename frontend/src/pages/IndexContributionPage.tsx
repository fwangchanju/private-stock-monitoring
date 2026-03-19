import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getIndexContribution } from '../api/dashboard'
import type { IndexContributionItem, MarketType } from '../types/api'
import { toPctSigned, signClass, toDateTimeLabel } from '../utils/format'

const MARKETS: MarketType[] = ['KOSPI', 'KOSDAQ']

export default function IndexContributionPage() {
  const [params, setParams] = useSearchParams()
  const market = (params.get('market') as MarketType) || 'KOSPI'

  const [items, setItems] = useState<IndexContributionItem[]>([])
  const [snapshotTime, setSnapshotTime] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    setError(null)
    getIndexContribution(market)
      .then(res => {
        setSnapshotTime(res.snapshotTime)
        setItems(res.items)
      })
      .catch(() => setError('데이터를 불러오지 못했습니다'))
      .finally(() => setLoading(false))
  }, [market])

  const set = (key: string, value: string) => {
    const next = new URLSearchParams(params)
    next.set(key, value)
    setParams(next)
  }

  return (
    <>
      <header className="app-header">
        <div className="page-title-bar">
          <Link to="/" className="back-link">← 대시보드</Link>
          <h2>지수 기여도 상위</h2>
        </div>
        {snapshotTime && (
          <div className="meta">
            <span>기준: {toDateTimeLabel(snapshotTime)}</span>
          </div>
        )}
      </header>
      <div className="page">
        <section className="section">
          <div className="section-header">
            <div className="actions">
              <div className="tab-bar">
                {MARKETS.map(m => (
                  <button
                    key={m}
                    className={`tab-btn ${market === m ? 'active' : ''}`}
                    onClick={() => set('market', m)}
                  >
                    {m}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {loading ? (
            <div className="loading">불러오는 중...</div>
          ) : error ? (
            <div className="empty-state">{error}</div>
          ) : items.length === 0 ? (
            <div className="empty-state">수집된 데이터가 없습니다</div>
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
                {items.map(item => (
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
      </div>
    </>
  )
}
