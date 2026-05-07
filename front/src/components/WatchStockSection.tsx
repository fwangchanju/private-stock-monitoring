import { Link } from 'react-router-dom'
import type { WatchStockItem } from '../types/api'

interface Props {
  items: WatchStockItem[]
}

export default function WatchStockSection({ items }: Props) {
  return (
    <section className="section">
      <div className="section-header">
        <h2>관심 종목</h2>
        <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{items.length}개</span>
      </div>
      {items.length === 0 ? (
        <div className="empty-state">등록된 관심 종목이 없습니다</div>
      ) : (
        <div className="watch-stock-list">
          {items.map(item => (
            <Link
              key={item.stockCode}
              to={`/stocks/${item.stockCode}`}
              className="watch-stock-chip"
              style={{ textDecoration: 'none', color: 'inherit' }}
            >
              <span>{item.stockName}</span>
              <span className="code">{item.stockCode}</span>
              <span style={{ fontSize: 10, color: 'var(--text-muted)' }}>{item.marketType}</span>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}
