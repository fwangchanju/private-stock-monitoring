import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { ProgramTradingRankingItem, ProgramRankingType } from '../types/api'
import { toEokSigned, signClass } from '../utils/format'

interface Props {
  items: ProgramTradingRankingItem[]
}

const RANKINGS: ProgramRankingType[] = ['NET_BUY', 'NET_SELL']

export default function ProgramTradingSection({ items }: Props) {
  const [ranking, setRanking] = useState<ProgramRankingType>('NET_BUY')

  const sorted = [...items]
    .filter(i => ranking === 'NET_BUY' ? i.programNetBuyAmount >= 0 : i.programNetBuyAmount < 0)
    .sort((a, b) =>
      ranking === 'NET_BUY'
        ? b.programNetBuyAmount - a.programNetBuyAmount
        : a.programNetBuyAmount - b.programNetBuyAmount,
    )
    .slice(0, 10)

  return (
    <section className="section">
      <div className="section-header">
        <h2>프로그램 매매 상위</h2>
        <div className="actions">
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
            to={`/program-trading?ranking=${ranking}`}
            style={{ fontSize: 12, color: 'var(--text-muted)' }}
          >
            전체 보기 →
          </Link>
        </div>
      </div>

      {items.length === 0 ? (
        <div className="empty-state">수집된 데이터가 없습니다</div>
      ) : sorted.length === 0 ? (
        <div className="empty-state">해당 조건의 데이터 없음</div>
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
            {sorted.map((item, idx) => (
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
  )
}
