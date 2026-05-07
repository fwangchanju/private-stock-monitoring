import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getProgramTradingRankings } from '../api/dashboard'
import type { ProgramTradingRankingItem, ProgramRankingType } from '../types/api'
import { toEokSigned, signClass, toDateTimeLabel } from '../utils/format'

const RANKINGS: ProgramRankingType[] = ['NET_BUY', 'NET_SELL']

export default function ProgramTradingPage() {
  const [params, setParams] = useSearchParams()
  const ranking = (params.get('ranking') as ProgramRankingType) || 'NET_BUY'

  const [items, setItems] = useState<ProgramTradingRankingItem[]>([])
  const [snapshotTime, setSnapshotTime] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    setError(null)
    getProgramTradingRankings(ranking)
      .then(res => {
        setSnapshotTime(res.snapshotTime)
        setItems(res.items)
      })
      .catch(() => setError('데이터를 불러오지 못했습니다'))
      .finally(() => setLoading(false))
  }, [ranking])

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
          <h2>프로그램 매매 상위</h2>
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
                {RANKINGS.map(r => (
                  <button
                    key={r}
                    className={`tab-btn ${ranking === r ? 'active' : ''}`}
                    onClick={() => set('ranking', r)}
                  >
                    {r === 'NET_BUY' ? '순매수' : '순매도'}
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
                  <th>프로그램순매수(억)</th>
                  <th>매수(억)</th>
                  <th>매도(억)</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, idx) => (
                  <tr key={item.stockCode}>
                    <td className="left" style={{ color: 'var(--text-muted)', fontSize: 11 }}>
                      {idx + 1}
                    </td>
                    <td className="left">
                      <Link to={`/stocks/${item.stockCode}`}>{item.stockName}</Link>
                      <span style={{ marginLeft: 6, fontSize: 11, color: 'var(--text-muted)' }}>
                        {item.stockCode}
                      </span>
                    </td>
                    <td className={signClass(item.programNetBuyAmount)}>
                      {toEokSigned(item.programNetBuyAmount)}
                    </td>
                    <td style={{ color: 'var(--text-muted)' }}>
                      {toEokSigned(item.programBuyAmount)}
                    </td>
                    <td style={{ color: 'var(--text-muted)' }}>
                      {toEokSigned(item.programSellAmount)}
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
