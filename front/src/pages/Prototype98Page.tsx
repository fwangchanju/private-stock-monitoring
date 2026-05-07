import { useEffect, useState } from 'react'
import '98.css'
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

const pos = (v: number) => ({ color: v > 0 ? '#0000aa' : v < 0 ? '#cc0000' : undefined })

export default function Prototype98Page() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [mktA, setMktA] = useState<MarketType>('KOSPI')
  const [invA, setInvA] = useState<InvestorType>('FOREIGNER')
  const [rankA, setRankA] = useState<IntradayRankingType>('NET_BUY')
  const [rankP, setRankP] = useState<ProgramRankingType>('NET_BUY')
  const [mktC, setMktC] = useState<MarketType>('KOSPI')

  useEffect(() => { getDashboard().then(setData).catch(() => setError('데이터를 불러오지 못했습니다')) }, [])

  if (error) return <div style={{ padding: 16 }}>{error}</div>
  if (!data) return <div style={{ padding: 16 }}>불러오는 중...</div>

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
    <div style={{ background: '#c0c0c0', minHeight: '100vh', padding: 12, fontFamily: 'sans-serif' }}>
      {/* 타이틀 바 */}
      <div className="window" style={{ marginBottom: 12 }}>
        <div className="title-bar">
          <div className="title-bar-text">PSMS Dashboard — {data.snapshotTime?.slice(0, 16).replace('T', ' ')} | {data.marketStatus ?? 'N/A'}</div>
          <div className="title-bar-controls">
            <button aria-label="Minimize" />
            <button aria-label="Maximize" />
            <button aria-label="Close" />
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>

        {/* 시장종합 */}
        <div className="window">
          <div className="title-bar">
            <div className="title-bar-text">시장종합</div>
          </div>
          <div className="window-body" style={{ padding: 8 }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr>
                  <th style={{ textAlign: 'left', padding: '2px 6px' }}>시장</th>
                  <th style={{ textAlign: 'right', padding: '2px 6px' }}>지수</th>
                  <th style={{ textAlign: 'right', padding: '2px 6px' }}>등락</th>
                  <th style={{ textAlign: 'right', padding: '2px 6px' }}>등락률</th>
                  <th style={{ textAlign: 'right', padding: '2px 6px' }}>거래대금(억)</th>
                  <th style={{ textAlign: 'right', padding: '2px 6px' }}>상승▲/하락▼</th>
                </tr>
              </thead>
              <tbody>
                {data.marketOverviews.map(item => (
                  <tr key={item.marketType} style={{ borderTop: '1px solid #999' }}>
                    <td style={{ padding: '3px 6px', fontWeight: 'bold' }}>{item.marketType}</td>
                    <td style={{ textAlign: 'right', padding: '3px 6px', ...pos(item.changeValue) }}>{toIndex(item.indexValue)}</td>
                    <td style={{ textAlign: 'right', padding: '3px 6px', ...pos(item.changeValue) }}>{item.changeValue > 0 ? '+' : ''}{toIndex(item.changeValue)}</td>
                    <td style={{ textAlign: 'right', padding: '3px 6px', ...pos(item.changeValue) }}>{toPctSigned(item.changeRate)}</td>
                    <td style={{ textAlign: 'right', padding: '3px 6px', color: '#555' }}>{toEok(item.tradingValue)}</td>
                    <td style={{ textAlign: 'right', padding: '3px 6px', fontSize: 11, color: '#555' }}>▲{item.advancers} / ▼{item.decliners}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* 관심 종목 */}
        <div className="window">
          <div className="title-bar">
            <div className="title-bar-text">관심 종목 ({data.watchStocks.length}개)</div>
          </div>
          <div className="window-body" style={{ padding: 8 }}>
            {data.watchStocks.length === 0 ? (
              <p>등록된 관심 종목이 없습니다.</p>
            ) : (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                {data.watchStocks.map(s => (
                  <button key={s.stockCode} style={{ fontSize: 11 }}>{s.stockName} ({s.stockCode})</button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* 투자자별 매매종합 */}
        <div className="window" style={{ gridColumn: '1 / -1' }}>
          <div className="title-bar">
            <div className="title-bar-text">투자자별 매매종합 (단위: 억원)</div>
          </div>
          <div className="window-body" style={{ padding: 8 }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr>
                  <th style={{ textAlign: 'left', padding: '2px 6px' }}>시장</th>
                  {INVESTORS.flatMap(inv => [
                    <th key={`${inv}-net`} style={{ textAlign: 'right', padding: '2px 6px' }}>{INVESTOR_LABEL[inv]} 순매수</th>,
                    <th key={`${inv}-buy`} style={{ textAlign: 'right', padding: '2px 6px', color: '#555' }}>매수</th>,
                  ])}
                </tr>
              </thead>
              <tbody>
                {MARKETS.map(mkt => (
                  <tr key={mkt} style={{ borderTop: '1px solid #999' }}>
                    <td style={{ padding: '3px 6px', fontWeight: 'bold' }}>{mkt}</td>
                    {INVESTORS.flatMap(inv => {
                      const d = data.investorTradingSummaries.find(i => i.marketType === mkt && i.investorType === inv)
                      const net = d?.netBuyAmount ?? 0
                      return [
                        <td key={`${inv}-net`} style={{ textAlign: 'right', padding: '3px 6px', ...pos(net) }}>{toEokSigned(net)}</td>,
                        <td key={`${inv}-buy`} style={{ textAlign: 'right', padding: '3px 6px', color: '#555' }}>{d ? toEokSigned(d.buyAmount) : '-'}</td>,
                      ]
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* 장중 투자자별 매매 상위 */}
        <div className="window">
          <div className="title-bar">
            <div className="title-bar-text">장중 투자자별 매매 상위</div>
          </div>
          <div className="window-body" style={{ padding: 8 }}>
            <div style={{ display: 'flex', gap: 4, marginBottom: 6, flexWrap: 'wrap' }}>
              {MARKETS.map(m => <button key={m} style={{ fontSize: 11, background: mktA === m ? '#000080' : undefined, color: mktA === m ? '#fff' : undefined }} onClick={() => setMktA(m)}>{m}</button>)}
              {INVESTORS.map(inv => <button key={inv} style={{ fontSize: 11, background: invA === inv ? '#000080' : undefined, color: invA === inv ? '#fff' : undefined }} onClick={() => setInvA(inv)}>{INVESTOR_LABEL[inv]}</button>)}
              {(['NET_BUY', 'NET_SELL'] as IntradayRankingType[]).map(r => <button key={r} style={{ fontSize: 11, background: rankA === r ? '#000080' : undefined, color: rankA === r ? '#fff' : undefined }} onClick={() => setRankA(r)}>{r === 'NET_BUY' ? '순매수' : '순매도'}</button>)}
            </div>
            {intradayFiltered.length === 0 ? <p style={{ color: '#555', fontSize: 12 }}>데이터 없음</p> : (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                <thead><tr>
                  <th style={{ textAlign: 'left', padding: '2px 4px' }}>#</th>
                  <th style={{ textAlign: 'left', padding: '2px 4px' }}>종목</th>
                  <th style={{ textAlign: 'right', padding: '2px 4px' }}>순매수(억)</th>
                  <th style={{ textAlign: 'right', padding: '2px 4px' }}>거래량</th>
                </tr></thead>
                <tbody>
                  {intradayFiltered.map(item => (
                    <tr key={item.rank} style={{ borderTop: '1px solid #ccc' }}>
                      <td style={{ padding: '2px 4px', color: '#555' }}>{item.rank}</td>
                      <td style={{ padding: '2px 4px' }}>{item.stockName} <span style={{ color: '#888', fontSize: 10 }}>{item.stockCode}</span></td>
                      <td style={{ textAlign: 'right', padding: '2px 4px', ...pos(item.netBuyAmount) }}>{toEokSigned(item.netBuyAmount)}</td>
                      <td style={{ textAlign: 'right', padding: '2px 4px', color: '#555' }}>{toVolume(item.tradedVolume)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* 프로그램 매매 상위 */}
        <div className="window">
          <div className="title-bar">
            <div className="title-bar-text">프로그램 매매 상위</div>
          </div>
          <div className="window-body" style={{ padding: 8 }}>
            <div style={{ display: 'flex', gap: 4, marginBottom: 6 }}>
              {(['NET_BUY', 'NET_SELL'] as ProgramRankingType[]).map(r => (
                <button key={r} style={{ fontSize: 11, background: rankP === r ? '#000080' : undefined, color: rankP === r ? '#fff' : undefined }} onClick={() => setRankP(r)}>{r === 'NET_BUY' ? '순매수' : '순매도'}</button>
              ))}
            </div>
            {programSorted.length === 0 ? <p style={{ color: '#555', fontSize: 12 }}>데이터 없음</p> : (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                <thead><tr>
                  <th style={{ textAlign: 'left', padding: '2px 4px' }}>#</th>
                  <th style={{ textAlign: 'left', padding: '2px 4px' }}>종목</th>
                  <th style={{ textAlign: 'right', padding: '2px 4px' }}>순매수(억)</th>
                  <th style={{ textAlign: 'right', padding: '2px 4px' }}>매수</th>
                  <th style={{ textAlign: 'right', padding: '2px 4px' }}>매도</th>
                </tr></thead>
                <tbody>
                  {programSorted.map((item, idx) => (
                    <tr key={item.stockCode} style={{ borderTop: '1px solid #ccc' }}>
                      <td style={{ padding: '2px 4px', color: '#555' }}>{idx + 1}</td>
                      <td style={{ padding: '2px 4px' }}>{item.stockName} <span style={{ color: '#888', fontSize: 10 }}>{item.stockCode}</span></td>
                      <td style={{ textAlign: 'right', padding: '2px 4px', ...pos(item.programNetBuyAmount) }}>{toEokSigned(item.programNetBuyAmount)}</td>
                      <td style={{ textAlign: 'right', padding: '2px 4px', color: '#555' }}>{toEokSigned(item.programBuyAmount)}</td>
                      <td style={{ textAlign: 'right', padding: '2px 4px', color: '#555' }}>{toEokSigned(item.programSellAmount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* 지수 기여도 상위 */}
        <div className="window" style={{ gridColumn: '1 / -1' }}>
          <div className="title-bar">
            <div className="title-bar-text">지수 기여도 상위</div>
          </div>
          <div className="window-body" style={{ padding: 8 }}>
            <div style={{ display: 'flex', gap: 4, marginBottom: 6 }}>
              {MARKETS.map(m => <button key={m} style={{ fontSize: 11, background: mktC === m ? '#000080' : undefined, color: mktC === m ? '#fff' : undefined }} onClick={() => setMktC(m)}>{m}</button>)}
            </div>
            {contribFiltered.length === 0 ? <p style={{ color: '#555', fontSize: 12 }}>데이터 없음</p> : (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                <thead><tr>
                  <th style={{ textAlign: 'left', padding: '2px 6px' }}>#</th>
                  <th style={{ textAlign: 'left', padding: '2px 6px' }}>종목</th>
                  <th style={{ textAlign: 'right', padding: '2px 6px' }}>기여도</th>
                  <th style={{ textAlign: 'right', padding: '2px 6px' }}>등락률</th>
                </tr></thead>
                <tbody>
                  {contribFiltered.map(item => (
                    <tr key={item.rank} style={{ borderTop: '1px solid #ccc' }}>
                      <td style={{ padding: '2px 6px', color: '#555' }}>{item.rank}</td>
                      <td style={{ padding: '2px 6px' }}>{item.stockName} <span style={{ color: '#888', fontSize: 10 }}>{item.stockCode}</span></td>
                      <td style={{ textAlign: 'right', padding: '2px 6px', ...pos(item.contributionScore) }}>{item.contributionScore.toFixed(2)}</td>
                      <td style={{ textAlign: 'right', padding: '2px 6px', ...pos(item.priceChangeRate) }}>{toPctSigned(item.priceChangeRate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

      </div>
    </div>
  )
}
