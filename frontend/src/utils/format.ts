/** 억 단위로 변환 (소수점 1자리) */
export const toEok = (value: number): string => {
  if (value === 0) return '0'
  return (value / 100_000_000).toFixed(1)
}

/** 부호 포함 억 단위 */
export const toEokSigned = (value: number): string => {
  const eok = value / 100_000_000
  const sign = eok > 0 ? '+' : ''
  return `${sign}${eok.toFixed(1)}`
}

/** 퍼센트 포맷 (소수점 2자리, 부호 포함) */
export const toPctSigned = (value: number): string => {
  const sign = value > 0 ? '+' : ''
  return `${sign}${value.toFixed(2)}%`
}

/** 퍼센트 포맷 (소수점 2자리) */
export const toPct = (value: number): string => `${value.toFixed(2)}%`

/** LocalDateTime(ISO) → 'HH:mm' */
export const toTimeLabel = (iso: string | null): string => {
  if (!iso) return '-'
  return iso.slice(11, 16)
}

/** LocalDateTime(ISO) → 'MM/DD HH:mm' */
export const toDateTimeLabel = (iso: string | null): string => {
  if (!iso) return '-'
  return `${iso.slice(5, 10)} ${iso.slice(11, 16)}`
}

/** LocalDate(ISO) → 'MM/DD' */
export const toDateLabel = (iso: string): string => iso.slice(5, 10)

/** 양수면 'positive', 음수면 'negative', 0이면 '' */
export const signClass = (value: number): string => {
  if (value > 0) return 'positive'
  if (value < 0) return 'negative'
  return ''
}

/** 지수 값 포맷 (소수점 2자리) */
export const toIndex = (value: number): string => value.toFixed(2)

/** 거래량 포맷 (천 단위 콤마) */
export const toVolume = (value: number): string =>
  value.toLocaleString('ko-KR')

/** 시장명 한글 */
export const marketLabel = (market: string): string =>
  market === 'KOSPI' ? 'KOSPI' : 'KOSDAQ'

/** 투자자 구분 한글 */
export const investorLabel = (type: string): string => {
  const map: Record<string, string> = {
    PERSONAL: '개인',
    FOREIGNER: '외국인',
    INSTITUTION: '기관',
  }
  return map[type] ?? type
}

/** 랭킹 구분 한글 */
export const rankingLabel = (type: string): string =>
  type === 'NET_BUY' ? '순매수' : '순매도'
