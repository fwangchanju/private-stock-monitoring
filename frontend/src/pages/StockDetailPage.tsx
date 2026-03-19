import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProgramTradingHistory, getShortSellingHistory } from '../api/dashboard'
import type { ProgramTradingHistoryItem, ShortSellingHistoryItem } from '../types/api'
import { toEokSigned, toVolume, toEok, toPct, signClass, toDateTimeLabel, toDateLabel } from '../utils/format'

export default function StockDetailPage() {
  const { stockCode } = useParams<{ stockCode: string }>()

  const [programHistory, setProgramHistory] = useState<ProgramTradingHistoryItem[]>([])
  const [shortHistory, setShortHistory] = useState<ShortSellingHistoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!stockCode) return
    setLoading(true)
    Promise.all([
      getProgramTradingHistory(stockCode),
      getShortSellingHistory(stockCode),
    ])
      .then(([prog, short]) => {
        setProgramHistory(prog.items)
        setShortHistory(short.items)
      })
      .catch(() => setError('데이터를 불러오지 못했습니다'))
      .finally(() => setLoading(false))
  }, [stockCode])

  return (
    <>
      <header className="app-header">
        <div className="page-title-bar">
          <Link to="/" className="back-link">← 대시보드</Link>
          <h2>종목 상세 — {stockCode}</h2>
        </div>
      </header>
      <div className="page">
        {loading ? (
          <div className="loading">불러오는 중...</div>
        ) : error ? (
          <div className="loading">{error}</div>
        ) : (
          <>
            <section className="section">
              <div className="section-header">
                <h2>프로그램 매매 추이</h2>
              </div>
              {programHistory.length === 0 ? (
                <div className="empty-state">수집된 데이터가 없습니다</div>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th className="left">시각</th>
                      <th>프로그램순매수(억)</th>
                      <th>매수(억)</th>
                      <th>매도(억)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {programHistory.map(item => (
                      <tr key={item.snapshotTime}>
                        <td className="left" style={{ color: 'var(--text-muted)' }}>
                          {toDateTimeLabel(item.snapshotTime)}
                        </td>
                        <td className={signClass(item.programNetBuyAmount)}>
                          {toEokSigned(item.programNetBuyAmount)}
                        </td>
                        <td style={{ color: 'var(--text-muted)' }}>
                          {toEok(item.programBuyAmount)}
                        </td>
                        <td style={{ color: 'var(--text-muted)' }}>
                          {toEok(item.programSellAmount)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>

            <section className="section">
              <div className="section-header">
                <h2>공매도 내역</h2>
              </div>
              {shortHistory.length === 0 ? (
                <div className="empty-state">수집된 데이터가 없습니다</div>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th className="left">일자</th>
                      <th>공매도량</th>
                      <th>공매도금액(억)</th>
                      <th>비중</th>
                    </tr>
                  </thead>
                  <tbody>
                    {shortHistory.map(item => (
                      <tr key={item.tradeDate}>
                        <td className="left" style={{ color: 'var(--text-muted)' }}>
                          {toDateLabel(item.tradeDate)}
                        </td>
                        <td style={{ color: 'var(--text-muted)' }}>
                          {toVolume(item.shortVolume)}
                        </td>
                        <td style={{ color: 'var(--text-muted)' }}>
                          {toEok(item.shortAmount)}
                        </td>
                        <td style={{ color: 'var(--text-muted)' }}>
                          {toPct(item.shortRatio)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          </>
        )}
      </div>
    </>
  )
}
