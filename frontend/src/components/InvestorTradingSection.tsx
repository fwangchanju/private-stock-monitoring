import type { InvestorTradingSummaryItem, MarketType, InvestorType } from '../types/api'
import { toEokSigned, signClass, investorLabel } from '../utils/format'

interface Props {
  items: InvestorTradingSummaryItem[]
}

const MARKETS: MarketType[] = ['KOSPI', 'KOSDAQ']
const INVESTORS: InvestorType[] = ['PERSONAL', 'FOREIGNER', 'INSTITUTION']

export default function InvestorTradingSection({ items }: Props) {
  const get = (market: MarketType, investor: InvestorType) =>
    items.find(i => i.marketType === market && i.investorType === investor)

  if (items.length === 0) {
    return (
      <section className="section">
        <div className="section-header"><h2>투자자별 매매종합</h2></div>
        <div className="empty-state">수집된 데이터가 없습니다</div>
      </section>
    )
  }

  return (
    <section className="section">
      <div className="section-header">
        <h2>투자자별 매매종합</h2>
        <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>단위: 억원</span>
      </div>
      <table className="data-table">
        <thead>
          <tr>
            <th className="left">시장</th>
            {INVESTORS.map(inv => (
              <th key={inv} colSpan={2}>{investorLabel(inv)}</th>
            ))}
          </tr>
          <tr>
            <th className="left"></th>
            {INVESTORS.flatMap(inv => [
              <th key={`${inv}-net`}>순매수</th>,
              <th key={`${inv}-buy`}>매수</th>,
            ])}
          </tr>
        </thead>
        <tbody>
          {MARKETS.map(market => (
            <tr key={market}>
              <td className="left">{market}</td>
              {INVESTORS.flatMap(investor => {
                const d = get(market, investor)
                const net = d?.netBuyAmount ?? 0
                return [
                  <td key={`${investor}-net`} className={signClass(net)}>
                    {toEokSigned(net)}
                  </td>,
                  <td key={`${investor}-buy`} style={{ color: 'var(--text-muted)' }}>
                    {d ? toEokSigned(d.buyAmount) : '-'}
                  </td>,
                ]
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
