# 구현된 기능 요약

기능이 변경되거나 새로 추가될 때 이 문서를 함께 현행화한다.

---

## 1. 시장종합 (Market Overview)

- 수집기: `MarketOverviewCollector` — `ka20001` (업종현재가요청) 호출, KOSPI/KOSDAQ 각 1회
- 저장 전략: `latest` — `market_overview` 테이블에 시장별 1건을 upsert
- 주요 수집 필드: 지수값, 등락값/율, 거래대금, 상한가/하한가 수, 상승/하락/보합 종목 수
- API: `GET /api/dashboard` 의 `marketOverviews` 배열로 제공
- 스케줄: 평일 09:00~15:59 5분마다 (`collectMarketData()` 내 포함)

---

## 2. 장중 투자자별 매매 상위 (Intraday Investor Ranking)

- 수집기: `IntradayInvestorRankingCollector` — `ka10065` (장중투자자별매매상위) 호출
- 저장 전략: `snapshot` — `intraday_investor_ranking_snapshot` 테이블에 시각별 스냅샷 누적
- 한 수집 사이클당 KOSPI×투자자 조합마다 금액/수량 2회 호출 (순매수금액 + 거래량 별도 추출)
- API: `GET /api/intraday-rankings?market=KOSPI&investor=FOREIGNER&ranking=NET_BUY`
- 스케줄: 평일 09:00~15:59 5분마다

---

## 3. 프로그램매매 순매수 상위 (Program Trading Ranking)

- 수집기: `ProgramTradingRankingCollector` — `ka90003` (프로그램순매수상위50) 호출
- 저장 전략: `snapshot` — `program_trading_ranking_snapshot` 테이블에 시각별 스냅샷 누적
- 한 수집 사이클당 8회 호출: KOSPI/KOSDAQ × 순매수/차익/비차익 × 금액/수량
- `stk_cd` 필드의 `_AL` 접미사 제거 후 저장
- API: `GET /api/program-trading-rankings?market=KOSPI&ranking=NET_BUY&amtQty=AMOUNT`
  - `market` 생략 시 KOSPI+KOSDAQ 합산 후 순매수 금액 기준 재정렬
- 스케줄: 평일 09:00~15:59 5분마다

---

## 4. 종목별 프로그램매매 추이 (Per-stock Program Trading History)

- 수집기: `ProgramTradingCollector`
  - 장중(`ka90008`): 종목별 KRX + NXT 각 1회 → 분 단위 버킷(`143433 → 143400`)으로 합산
  - 일별(`ka90013`): 종목별 KRX + NXT 각 1회 → 날짜 기준 합산
- 저장 전략: `history` — `program_trading_history`(분봉) / `program_trading_daily`(일별) 테이블
- 중복 저장 방지: `existsByStockCodeAndSnapshotTime` / `existsByStockCodeAndTradeDate` 로 사전 체크
- API:
  - `GET /api/stocks/{stockCode}/program-trading?from=...&to=...` (장중)
  - `GET /api/stocks/{stockCode}/program-trading/daily?from=...&to=...` (일별)
- 스케줄: 장중 5분마다 / 일별은 평일 16:00 1회

---

## 5. 종목별 공매도 추이 (Per-stock Short Selling History)

- 수집기: `ShortSellingCollector` — `ka10014` (공매도추이) 호출
- 종목별 KRX + NXT 각 1회 → 날짜별 수량/금액 합산, 가격 계산 필드(평균가·비율)는 KRX 기준 재계산
- 저장 전략: `history` — `short_selling_history` 테이블에 upsert (이미 있으면 `update()`)
- 주요 수집 필드: 공매도 수량, 잔고수량, 거래대금, 평균가, 비율, 종가, 등락값/율
- API: `GET /api/stocks/{stockCode}/short-selling?from=...&to=...`
- 스케줄: 평일 19:00 1회 (장 마감 후 데이터 확정 이후)
