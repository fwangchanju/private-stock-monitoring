import { useEffect, useState } from 'react'
import { getDashboard } from '../api/dashboard'
import type {
  DashboardResponse,
  InvestorType,
  MarketType,
  IntradayRankingType,
  ProgramRankingType,
} from '../types/api'
import { toEok, toEokSigned, toIndex, toPctSigned, toVolume } from '../utils/format'

const S = {
  root: {
    background: '#0a0a0a',
    color: '#00ff41',
    fontFamily: '"Courier New", Courier, monospace',
    fontSize: 13,
    minHeight: '100vh',
    padding: '12px 16px',
  } as React.CSSProperties,
  header: {
    borderBottom: '1px solid #00ff41',
    marginBottom: 12,
    paddingBottom: 6,
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'baseline',
  } as React.CSSProperties,
  h1: { fontSize: 15, fontWeight: 'bold', margin: 0, letterSpacing: 2 } as React.CSSProperties,
  meta: { fontSize: 11, color: '#00aa29' } as React.CSSProperties,
  section: { border: '1px solid #00a832', marginBottom: 12, padding: '6px 10px' } as React.CSSProperties,
  sectionTitle: {
    background: '#00ff41',
    color: '#000',
    padding: '1px 8px',
    marginBottom: 8,
    fontSize: 12,
    fontWeight: 'bold',
    letterSpacing: 1,
  } as React.CSSProperties,
  table: { width: '100%', borderCollapse: 'collapse' as const, fontSize: 12 },
  th: {
    textAlign: 'left' as const,
    padding: '2px 6px',
    borderBottom: '1px dashed #00a832',
    color: '#00a832',
    fontWeight: 'normal',
  } as React.CSSProperties,
  thRight: {
    textAlign: 'right' as const,
    padding: '2px 6px',
    borderBottom: '1px dashed #00a832',
    color: '#00a832',
    fontWeight: 'normal',
  } as React.CSSProperties,
  td: { padding: '2px 6px', textAlign: 'left' as const } as React.CSSProperties,
  tdRight: { padding: '2px 6px', textAlign: 'right' as const } as React.CSSProperties,
  pos: { color: '#00ff41' } as React.CSSProperties,
  neg: { color: '#ff4444' } as React.CSSProperties,
  muted: { color: '#006622' } as React.CSSProperties,
  tabRow: { display: 'flex', gap: 2, marginBottom: 6 } as React.CSSProperties,
  tabBtn: (active: boolean): React.CSSProperties => ({
    background: active ? '#00ff41' : 'transparent',
    color: active ? '#000' : '#00a832',
    border: '1px solid #00a832',
    padding: '1px 8px',
    fontSize: 11,
    fontFamily: 'inherit',
    cursor: 'pointer',
  }),
  chips: { display: 'flex', flexWrap: 'wrap' as const, gap: 6 } as React.CSSProperties,
  chip: { border: '1px solid #00a832', padding: '2px 8px', fontSize: 12, color: '#00ff41', textDecoration: 'none' } as React.CSSProperties,
}

const MARKETS: MarketType[] = ['KOSPI', 'KOSDAQ']
const INVESTORS: InvestorType[] = ['PERSONAL', 'FOREIGNER', 'INSTITUTION']
const INVESTOR_LABEL: Record<string, string> = { PERSONAL: '개인', FOREIGNER: '외국인', INSTITUTION: '기관' }

export default function PrototypeDosPage() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [mktA, setMktA] = useState<MarketType>('KOSPI')
  const [invA, setInvA] = useState<InvestorType>('FOREIGNER')
  const [rankA, setRankA] = useState<IntradayRankingType>('NET_BUY')
  const [rankP, setRankP] = useState<ProgramRankingType>('NET_BUY')
  const [mktC, setMktC] = useState<MarketType>('KOSPI')

  useEffect(() => { getDashboard().then(setData).catch(() => setError('데이터 로드 실패')) }, [])

  if (error) return <div style={S.root}>{error}</div>
  if (!data) return <div style={S.root}>C:\PSMS&gt; Loading dashboard... _</div>

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
    <div style={S.root}>
      <header style={S.header}>
        <h1 style={S.h1}>C:\PSMS&gt; DASHBOARD.EXE</h1>
        <span style={S.meta}>
          {data.snapshotTime?.slice(0, 16).replace('T', ' ')} | {data.marketStatus ?? 'N/A'}
        </span>
      </header>

      {/* 시장종합 */}
      <section style={S.section}>
        <div style={S.sectionTitle}>[ 시장종합 ]</div>
        <table style={S.table}>
          <thead>
            <tr>
              <th style={S.th}>시장</th>
              <th style={S.thRight}>지수</th>
              <th style={S.thRight}>등락</th>
              <th style={S.thRight}>등락률</th>
              <th style={S.thRight}>거래대금(억)</th>
              <th style={S.thRight}>상승/하락/보합</th>
            </tr>
          </thead>
          <tbody>
            {data.marketOverviews.map(item => (
              <tr key={item.marketType}>
                <td style={S.td}>{item.marketType}</td>
                <td style={{ ...S.tdRight, ...(item.changeValue > 0 ? S.pos : item.changeValue < 0 ? S.neg : {}) }}>
                  {toIndex(item.indexValue)}
                </td>
                <td style={{ ...S.tdRight, ...(item.changeValue > 0 ? S.pos : item.changeValue < 0 ? S.neg : {}) }}>
                  {item.changeValue > 0 ? '+' : ''}{toIndex(item.changeValue)}
                </td>
                <td style={{ ...S.tdRight, ...(item.changeValue > 0 ? S.pos : item.changeValue < 0 ? S.neg : {}) }}>
                  {toPctSigned(item.changeRate)}
                </td>
                <td style={{ ...S.tdRight, ...S.muted }}>{toEok(item.tradingValue)}</td>
                <td style={{ ...S.tdRight, ...S.muted }}>
                  +{item.advancers} / -{item.decliners} / {item.unchangedCount}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {/* 투자자별 매매종합 */}
      <section style={S.section}>
        <div style={S.sectionTitle}>[ 투자자별 매매종합 ] (단위: 억원)</div>
        <table style={S.table}>
          <thead>
            <tr>
              <th style={S.th}>시장</th>
              {INVESTORS.flatMap(inv => [
                <th key={`${inv}-net`} style={S.thRight}>{INVESTOR_LABEL[inv]} 순매수</th>,
                <th key={`${inv}-buy`} style={S.thRight}>매수</th>,
              ])}
            </tr>
          </thead>
          <tbody>
            {MARKETS.map(mkt => (
              <tr key={mkt}>
                <td style={S.td}>{mkt}</td>
                {INVESTORS.flatMap(inv => {
                  const d = data.investorTradingSummaries.find(i => i.marketType === mkt && i.investorType === inv)
                  const net = d?.netBuyAmount ?? 0
                  return [
                    <td key={`${inv}-net`} style={{ ...S.tdRight, ...(net > 0 ? S.pos : net < 0 ? S.neg : {}) }}>
                      {toEokSigned(net)}
                    </td>,
                    <td key={`${inv}-buy`} style={{ ...S.tdRight, ...S.muted }}>
                      {d ? toEokSigned(d.buyAmount) : '-'}
                    </td>,
                  ]
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {/* 장중 투자자별 매매 상위 */}
      <section style={S.section}>
        <div style={S.sectionTitle}>[ 장중 투자자별 매매 상위 ]</div>
        <div style={S.tabRow}>
          {MARKETS.map(m => <button key={m} style={S.tabBtn(mktA === m)} onClick={() => setMktA(m)}>{m}</button>)}
          <span style={{ ...S.muted, padding: '0 4px' }}>|</span>
          {INVESTORS.map(inv => <button key={inv} style={S.tabBtn(invA === inv)} onClick={() => setInvA(inv)}>{INVESTOR_LABEL[inv]}</button>)}
          <span style={{ ...S.muted, padding: '0 4px' }}>|</span>
          {(['NET_BUY', 'NET_SELL'] as IntradayRankingType[]).map(r => (
            <button key={r} style={S.tabBtn(rankA === r)} onClick={() => setRankA(r)}>{r === 'NET_BUY' ? '순매수' : '순매도'}</button>
          ))}
        </div>
        {intradayFiltered.length === 0 ? <span style={S.muted}>데이터 없음</span> : (
          <table style={S.table}>
            <thead>
              <tr>
                <th style={S.th}>#</th><th style={S.th}>종목</th>
                <th style={S.thRight}>순매수(억)</th><th style={S.thRight}>거래량</th>
              </tr>
            </thead>
            <tbody>
              {intradayFiltered.map(item => (
                <tr key={item.rank}>
                  <td style={{ ...S.td, ...S.muted }}>{item.rank}</td>
                  <td style={S.td}>{item.stockName} <span style={S.muted}>{item.stockCode}</span></td>
                  <td style={{ ...S.tdRight, ...(item.netBuyAmount > 0 ? S.pos : S.neg) }}>{toEokSigned(item.netBuyAmount)}</td>
                  <td style={{ ...S.tdRight, ...S.muted }}>{toVolume(item.tradedVolume)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {/* 프로그램 매매 상위 */}
      <section style={S.section}>
        <div style={S.sectionTitle}>[ 프로그램 매매 상위 ]</div>
        <div style={S.tabRow}>
          {(['NET_BUY', 'NET_SELL'] as ProgramRankingType[]).map(r => (
            <button key={r} style={S.tabBtn(rankP === r)} onClick={() => setRankP(r)}>{r === 'NET_BUY' ? '순매수' : '순매도'}</button>
          ))}
        </div>
        {programSorted.length === 0 ? <span style={S.muted}>데이터 없음</span> : (
          <table style={S.table}>
            <thead>
              <tr>
                <th style={S.th}>#</th><th style={S.th}>종목</th>
                <th style={S.thRight}>프로그램순매수(억)</th><th style={S.thRight}>매수</th><th style={S.thRight}>매도</th>
              </tr>
            </thead>
            <tbody>
              {programSorted.map((item, idx) => (
                <tr key={item.stockCode}>
                  <td style={{ ...S.td, ...S.muted }}>{idx + 1}</td>
                  <td style={S.td}>{item.stockName} <span style={S.muted}>{item.stockCode}</span></td>
                  <td style={{ ...S.tdRight, ...(item.programNetBuyAmount > 0 ? S.pos : S.neg) }}>{toEokSigned(item.programNetBuyAmount)}</td>
                  <td style={{ ...S.tdRight, ...S.muted }}>{toEokSigned(item.programBuyAmount)}</td>
                  <td style={{ ...S.tdRight, ...S.muted }}>{toEokSigned(item.programSellAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {/* 지수 기여도 상위 */}
      <section style={S.section}>
        <div style={S.sectionTitle}>[ 지수 기여도 상위 ]</div>
        <div style={S.tabRow}>
          {MARKETS.map(m => <button key={m} style={S.tabBtn(mktC === m)} onClick={() => setMktC(m)}>{m}</button>)}
        </div>
        {contribFiltered.length === 0 ? <span style={S.muted}>데이터 없음</span> : (
          <table style={S.table}>
            <thead>
              <tr>
                <th style={S.th}>#</th><th style={S.th}>종목</th>
                <th style={S.thRight}>기여도</th><th style={S.thRight}>등락률</th>
              </tr>
            </thead>
            <tbody>
              {contribFiltered.map(item => (
                <tr key={item.rank}>
                  <td style={{ ...S.td, ...S.muted }}>{item.rank}</td>
                  <td style={S.td}>{item.stockName} <span style={S.muted}>{item.stockCode}</span></td>
                  <td style={{ ...S.tdRight, ...(item.contributionScore > 0 ? S.pos : S.neg) }}>{item.contributionScore.toFixed(2)}</td>
                  <td style={{ ...S.tdRight, ...(item.priceChangeRate > 0 ? S.pos : S.neg) }}>{toPctSigned(item.priceChangeRate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {/* 관심 종목 */}
      <section style={S.section}>
        <div style={S.sectionTitle}>[ 관심 종목 ] ({data.watchStocks.length}개)</div>
        {data.watchStocks.length === 0 ? (
          <span style={S.muted}>등록된 관심 종목 없음</span>
        ) : (
          <div style={S.chips}>
            {data.watchStocks.map(s => (
              <span key={s.stockCode} style={S.chip}>{s.stockName} <span style={S.muted}>{s.stockCode}</span></span>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
