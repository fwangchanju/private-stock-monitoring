import type { MarketOverviewItem } from '../types/api'
import { toIndex, toEok, toPctSigned, signClass } from '../utils/format'

interface Props {
  items: MarketOverviewItem[]
}

export default function MarketOverviewSection({ items }: Props) {
  if (items.length === 0) {
    return (
      <section className="section">
        <div className="section-header"><h2>시장종합</h2></div>
        <div className="empty-state">수집된 데이터가 없습니다</div>
      </section>
    )
  }

  return (
    <section className="section">
      <div className="section-header">
        <h2>시장종합</h2>
      </div>
      <div className="market-cards">
        {items.map(item => (
          <div key={item.marketType} className="market-card">
            <div className="label">{item.marketType}</div>
            <div className={`index-value ${signClass(item.changeValue)}`}>
              {toIndex(item.indexValue)}
            </div>
            <div className={`change-row ${signClass(item.changeValue)}`}>
              {toPctSigned(item.changeRate)} &nbsp; {item.changeValue > 0 ? '+' : ''}{toIndex(item.changeValue)}
            </div>
            <div className="stats-row">
              <span>거래대금 {toEok(item.tradingValue)}억</span>
              <span className="positive">▲{item.advancers}</span>
              <span className="negative">▼{item.decliners}</span>
              <span>—{item.unchangedCount}</span>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
