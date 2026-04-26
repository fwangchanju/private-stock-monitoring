import { useEffect, useState } from 'react'
import 'nes.css/css/nes.min.css'
import { getDashboard } from '../api/dashboard'
import type {
  DashboardResponse,
  InvestorType,
  MarketType,
  IntradayRankingType,
  ProgramRankingType,
} from '../types/api'
import { toEok, toEokSigned, toIndex, toPctSigned, toVolume } from '../utils/format'

const MARKETS: MarketType[] = ['KOSPI', 'KOSDAQ']
const INVESTORS: InvestorType[] = ['PERSONAL', 'FOREIGNER', 'INSTITUTION']
const INVESTOR_LABEL: Record<string, string> = { PERSONAL: '개인', FOREIGNER: '외국인', INSTITUTION: '기관' }

const colorClass = (v: number) => v > 0 ? 'nes-text is-success' : v < 0 ? 'nes-text is-error' : ''

export default function PrototypeNesPage() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [mktA, setMktA] = useState<MarketType>('KOSPI')
  const [invA, setInvA] = useState<InvestorType>('FOREIGNER')
  const [rankA, setRankA] = useState<IntradayRankingType>('NET_BUY')
  const [rankP, setRankP] = useState<ProgramRankingType>('NET_BUY')
  const [mktC, setMktC] = useState<MarketType>('KOSPI')

  useEffect(() => {
    getDashboard().then(setData).catch(() => setError('데이터를 불러오지 못했습니다'))
    // Press Start 2P 폰트 로드
    const link = document.createElement('link')
    link.rel = 'stylesheet'
    link.href = 'https://fonts.googleapis.com/css?family=Press+Start+2P'
    link.id = 'nes-font'
    document.head.appendChild(link)
    return () => document.getElementById('nes-font')?.remove()
  }, [])

  if (error) return <div style={{ padding: 16 }}>{error}</div>
  if (!data) return (
    <div style={{ background: '#212529', minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <span className="nes-text is-primary" style={{ fontFamily: '"Press Start 2P"' }}>LOADING...</span>
    </div>
  )

  const intradayFiltered = data.intradayTopRankings
    .filter(i => i.marketType === mktA && i.investorType === invA)
    .slice(0, 10)

  const programSorted = [...data.programTradingHighlights]
    .filter(i => rankP === 'NET_BUY' ? i.programNetBuyAmount >= 0 : i.programNetBuyAmount < 0)
    .sort((a, b) => rankP === 'NET_BUY'
      ? b.programNetBuyAmount - a.programNetBuyAmount
      : a.programNetBuyAmount - b.programNetBuyAmount)
    .slice(0, 10)

  const contribFiltered = data.indexContributionHighlights
    .filter(i => i.marketType === mktC)
    .sort((a, b) => a.rank - b.rank)
    .slice(0, 10)

  return (
    <div style={{ background: '#212529', minHeight: '100vh', padding: 16, fontFamily: '"Press Start 2P", monospace', color: '#fff' }}>
      {/* 헤더 */}
      <div style={{ marginBottom: 16, borderBottom: '4px solid #92cc41', paddingBottom: 8 }}>
        <span className="nes-text is-primary" style={{ fontSize: 14 }}>PSMS DASHBOARD</span>
        <span style={{ fontSize: 9, color: '#aaa', marginLeft: 16 }}>
          {data.snapshotTime?.slice(0, 16).replace('T', ' ')} | {data.marketStatus ?? 'N/A'}
        </span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>

        {/* 시장종합 */}
        <div className="nes-container with-title" style={{ gridColumn: '1 / -1' }}>
          <p className="title">시장종합</p>
          <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
            {data.marketOverviews.map(item => (
              <div key={item.marketType} className="nes-container is-rounded" style={{ flex: 1, minWidth: 200, background: '#111' }}>
                <p style={{ fontSize: 11, marginBottom: 4, color: '#92cc41' }}>{item.marketType}</p>
                <p className={colorClass(item.changeValue)} style={{ fontSize: 16, margin: '4px 0' }}>
                  {toIndex(item.indexValue)}
                </p>
                <p className={colorClass(item.changeValue)} style={{ fontSize: 9, margin: '2px 0' }}>
                  {item.changeValue > 0 ? '+' : ''}{toIndex(item.changeValue)} ({toPctSigned(item.changeRate)})
                </p>
                <p style={{ fontSize: 9, color: '#888', margin: '4px 0' }}>
                  거래대금 {toEok(item.tradingValue)}억
                </p>
                <p style={{ fontSize: 9, margin: '2px 0' }}>
                  <span className="nes-text is-success">▲{item.advancers}</span>
                  {' '}<span className="nes-text is-error">▼{item.decliners}</span>
                  {' '}<span style={{ color: '#888' }}>—{item.unchangedCount}</span>
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* 투자자별 매매종합 */}
        <div className="nes-container with-title" style={{ gridColumn: '1 / -1' }}>
          <p className="title">투자자별 매매종합</p>
          <div style={{ overflowX: 'auto' }}>
            <table className="nes-table is-bordered" style={{ width: '100%', fontSize: 9 }}>
              <thead>
                <tr>
                  <th>시장</th>
                  {INVESTORS.flatMap(inv => [
                    <th key={`${inv}-net`}>{INVESTOR_LABEL[inv]} 순매수</th>,
                    <th key={`${inv}-buy`} style={{ color: '#aaa' }}>매수</th>,
                  ])}
                </tr>
              </thead>
              <tbody>
                {MARKETS.map(mkt => (
                  <tr key={mkt}>
                    <td>{mkt}</td>
                    {INVESTORS.flatMap(inv => {
                      const d = data.investorTradingSummaries.find(i => i.marketType === mkt && i.investorType === inv)
                      const net = d?.netBuyAmount ?? 0
                      return [
                        <td key={`${inv}-net`}><span className={colorClass(net)}>{toEokSigned(net)}</span></td>,
                        <td key={`${inv}-buy`} style={{ color: '#aaa' }}>{d ? toEokSigned(d.buyAmount) : '-'}</td>,
                      ]
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* 장중 투자자별 매매 상위 */}
        <div className="nes-container with-title">
          <p className="title">장중 투자자 상위</p>
          <div style={{ display: 'flex', gap: 4, marginBottom: 8, flexWrap: 'wrap' }}>
            {MARKETS.map(m => (
              <button key={m} className={`nes-btn ${mktA === m ? 'is-primary' : ''}`} style={{ fontSize: 8, padding: '4px 8px' }} onClick={() => setMktA(m)}>{m}</button>
            ))}
            {INVESTORS.map(inv => (
              <button key={inv} className={`nes-btn ${invA === inv ? 'is-success' : ''}`} style={{ fontSize: 8, padding: '4px 8px' }} onClick={() => setInvA(inv)}>{INVESTOR_LABEL[inv]}</button>
            ))}
            {(['NET_BUY', 'NET_SELL'] as IntradayRankingType[]).map(r => (
              <button key={r} className={`nes-btn ${rankA === r ? 'is-warning' : ''}`} style={{ fontSize: 8, padding: '4px 8px' }} onClick={() => setRankA(r)}>{r === 'NET_BUY' ? '순매수' : '순매도'}</button>
            ))}
          </div>
          {intradayFiltered.length === 0 ? <p style={{ fontSize: 9, color: '#888' }}>데이터 없음</p> : (
            <table className="nes-table is-bordered" style={{ width: '100%', fontSize: 9 }}>
              <thead><tr><th>#</th><th>종목</th><th>순매수(억)</th><th>거래량</th></tr></thead>
              <tbody>
                {intradayFiltered.map(item => (
                  <tr key={item.rank}>
                    <td style={{ color: '#888' }}>{item.rank}</td>
                    <td>{item.stockName}<br /><span style={{ color: '#666', fontSize: 8 }}>{item.stockCode}</span></td>
                    <td><span className={colorClass(item.netBuyAmount)}>{toEokSigned(item.netBuyAmount)}</span></td>
                    <td style={{ color: '#888' }}>{toVolume(item.tradedVolume)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* 프로그램 매매 상위 */}
        <div className="nes-container with-title">
          <p className="title">프로그램 매매 상위</p>
          <div style={{ display: 'flex', gap: 4, marginBottom: 8 }}>
            {(['NET_BUY', 'NET_SELL'] as ProgramRankingType[]).map(r => (
              <button key={r} className={`nes-btn ${rankP === r ? 'is-primary' : ''}`} style={{ fontSize: 8, padding: '4px 8px' }} onClick={() => setRankP(r)}>{r === 'NET_BUY' ? '순매수' : '순매도'}</button>
            ))}
          </div>
          {programSorted.length === 0 ? <p style={{ fontSize: 9, color: '#888' }}>데이터 없음</p> : (
            <table className="nes-table is-bordered" style={{ width: '100%', fontSize: 9 }}>
              <thead><tr><th>#</th><th>종목</th><th>순매수(억)</th><th>매수</th><th>매도</th></tr></thead>
              <tbody>
                {programSorted.map((item, idx) => (
                  <tr key={item.stockCode}>
                    <td style={{ color: '#888' }}>{idx + 1}</td>
                    <td>{item.stockName}<br /><span style={{ color: '#666', fontSize: 8 }}>{item.stockCode}</span></td>
                    <td><span className={colorClass(item.programNetBuyAmount)}>{toEokSigned(item.programNetBuyAmount)}</span></td>
                    <td style={{ color: '#aaa' }}>{toEokSigned(item.programBuyAmount)}</td>
                    <td style={{ color: '#aaa' }}>{toEokSigned(item.programSellAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* 지수 기여도 상위 */}
        <div className="nes-container with-title">
          <p className="title">지수 기여도 상위</p>
          <div style={{ display: 'flex', gap: 4, marginBottom: 8 }}>
            {MARKETS.map(m => (
              <button key={m} className={`nes-btn ${mktC === m ? 'is-primary' : ''}`} style={{ fontSize: 8, padding: '4px 8px' }} onClick={() => setMktC(m)}>{m}</button>
            ))}
          </div>
          {contribFiltered.length === 0 ? <p style={{ fontSize: 9, color: '#888' }}>데이터 없음</p> : (
            <table className="nes-table is-bordered" style={{ width: '100%', fontSize: 9 }}>
              <thead><tr><th>#</th><th>종목</th><th>기여도</th><th>등락률</th></tr></thead>
              <tbody>
                {contribFiltered.map(item => (
                  <tr key={item.rank}>
                    <td style={{ color: '#888' }}>{item.rank}</td>
                    <td>{item.stockName}<br /><span style={{ color: '#666', fontSize: 8 }}>{item.stockCode}</span></td>
                    <td><span className={colorClass(item.contributionScore)}>{item.contributionScore.toFixed(2)}</span></td>
                    <td><span className={colorClass(item.priceChangeRate)}>{toPctSigned(item.priceChangeRate)}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* 관심 종목 */}
        <div className="nes-container with-title">
          <p className="title">관심 종목</p>
          {data.watchStocks.length === 0 ? (
            <p style={{ fontSize: 9, color: '#888' }}>등록된 관심 종목이 없습니다</p>
          ) : (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {data.watchStocks.map(s => (
                <span key={s.stockCode} className="nes-badge" style={{ fontSize: 8 }}>
                  <span className="is-primary">{s.stockName}</span>
                </span>
              ))}
            </div>
          )}
        </div>

      </div>
    </div>
  )
}
