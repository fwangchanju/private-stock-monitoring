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

## 2. 투자자별 매매종합 (Investor Trading Summary)

- 수집기: `InvestorTradingSummaryCollector` — `ka10051` (업종별투자자순매수요청) 호출
- 저장 전략: `latest` — `investor_trading_summary` 테이블에 시장×투자자 조합별 1건 upsert
- 업종코드 001(코스피종합) / 101(코스닥종합) 기준 조회, `stex_tp=1` (KRX only)
- 수집 투자자 13종: 개인, 외국인, 기관계, 금융투자, 투신, 연기금, 사모펀드, 보험, 은행, 기타법인, 국가, 종금, 국내처리외국인
- API: `GET /api/dashboard` 의 `investorTradingSummaries` 배열로 제공
- 스케줄: 평일 09:00~15:59 5분마다

---

## 3. 장중 투자자별 매매 상위 (Intraday Investor Ranking)

- 수집기: `IntradayInvestorRankingCollector` — `ka10065` (장중투자자별매매상위) 호출
- 저장 전략: `snapshot` — `intraday_investor_ranking_snapshot` 테이블에 시각별 스냅샷 누적
- 한 수집 사이클당 KOSPI/KOSDAQ × orgn_tp 11종 × 순매수/순매도 = 44회 호출
- orgn_tp 코드: 9000(외국인), 9999(기관계), 1000(금융투자), 2000(보험), 3000(투신), 4000(은행), 5000(기타금융), 6000(연기금), 7000(국가), 7100(기타법인), 9100(외국계)
- API: `GET /api/intraday-rankings?market=KOSPI&investor=FOREIGNER&ranking=NET_BUY`
- 스케줄: 평일 09:00~15:59 5분마다

---

## 4. 프로그램매매 순매수 상위 (Program Trading Ranking)

- 수집기: `ProgramTradingRankingCollector` — `ka90003` (프로그램순매수상위50) 호출
- 저장 전략: `snapshot` — `program_trading_ranking_snapshot` 테이블에 시각별 스냅샷 누적
- 한 수집 사이클당 8회 호출: KOSPI/KOSDAQ × 순매수/차익/비차익 × 금액/수량
- `stk_cd` 필드의 `_AL` 접미사 제거 후 저장
- API: `GET /api/program-trading-rankings?market=KOSPI&ranking=NET_BUY&amtQty=AMOUNT`
  - `market` 생략 시 KOSPI+KOSDAQ 합산 후 순매수 금액 기준 재정렬
- 스케줄: 평일 09:00~15:59 5분마다

---

## 5. 종목별 프로그램매매 추이 (Per-stock Program Trading History)

- 수집기: `ProgramTradingCollector`
  - 장중(`ka90008`): 종목별 KRX + NXT 각 1회 → 분 단위 버킷(`143433 → 143400`)으로 합산
  - 일별(`ka90013`): 종목별 KRX + NXT 각 1회 → 날짜 기준 합산
- 저장 전략: `history` — `program_trading_history`(분봉) / `program_trading_daily`(일별) 테이블
- 중복 저장 방지: `existsByStockCodeAndSnapshotTime` / `existsByStockCodeAndTradeDate` 로 사전 체크
- 수집 대상: 관심종목(`WatchStock`) 기준
- API:
  - `GET /api/stocks/{stockCode}/program-trading?from=...&to=...` (장중)
  - `GET /api/stocks/{stockCode}/program-trading/daily?from=...&to=...` (일별)
- 스케줄: 장중 5분마다 / 일별은 평일 16:00 1회

---

## 6. 종목별 공매도 추이 (Per-stock Short Selling History)

- 수집기: `ShortSellingCollector` — KRX 크롤링 (`MDCSTAT30101`)
- 코스피(STK) + 코스닥(KSQ) 전종목을 2회 호출로 수집 후 관심종목(`WatchStock`)만 저장
- 저장 전략: `history` — `short_selling_history` 테이블에 upsert (날짜×종목 키)
- 주요 수집 필드: 공매도 거래량(`CVSRTSELL_TRDVOL`), 거래대금(`CVSRTSELL_TRDVAL`), 비중(`TRDVOL_WT`)
- 수집 실패 시 당일 데이터 영구 결손 → `EscalateException` 발생
- API: `GET /api/stocks/{stockCode}/short-selling?from=...&to=...`
- 스케줄: 평일 19:00 1회 (KRX 당일 데이터 18:30 이후 확정)

---

## 7. 지수기여도 상위 (Index Contribution Ranking)

- 수집기: `IndexContributionRankingCollector` — KRX 크롤링 (`MDCSTAT01501`, 전종목 시세)
- 저장 전략: `snapshot` — `index_contribution_ranking_snapshot` 테이블에 시각별 누적
- 수집 대상: KOSPI/KOSDAQ 각각 상위 50종목
- 기여도 연산 공식: `(현재가 - 전일종가) × 상장주식수 / 전일 전체 시가총액 × 전일 지수값`
  - 전일 지수값: `MarketOverview.indexValue - MarketOverview.changeValue` 로 역산
  - 중복 스냅샷 방지: `snapshotTime` 기준 이미 존재하면 스킵
- API: `GET /api/index-contribution-rankings?market=KOSPI&snapshotTime=...`
- 스케줄: 평일 09:00~15:59 5분마다

---

## 8. KRX 크롤링 공통 모듈

- `KrxCrawler`: `POST data.krx.co.kr/comm/bldAttendant/getJsonData.cmd`
- `bld` 파라미터로 데이터 종류 구분, `User-Agent` 헤더 필요
- 응답의 `OutBlock_1` 배열을 `List<JsonNode>`로 반환
- `OutBlock_1` 누락 또는 null → KRX 인터페이스 구조 변경으로 판단하여 `EscalateException` 발생

---

## 9. 예외 처리 체계

- `BusinessException` (최상위, `RuntimeException` 상속): 공통 핸들러에서 로그만 기록
- `EscalateException` (`BusinessException` 상속): 핸들러에서 `AlertService`를 통해 `DEVELOPER_CHAT_ID`로 텔레그램 즉시 발송
- `AlertService`: HTTP 요청/스케줄러 양쪽 컨텍스트에서 호출 가능한 발송 서비스
- `CollectionScheduler.runSafely()`: 모든 수집기 예외를 캐치하되 `EscalateException`은 `AlertService`로 전달

---

## 10. 텔레그램 봇 — 관심종목 관리

- `TelegramPollingService`: `getUpdates` long-polling (3초 간격), 허용 chatId 검증
- `WatchStockBotHandler`: 명령 파싱 + `WatchStockRepository` CRUD 연동
- 대화 상태: `ConcurrentHashMap<String, ConversationState>` (앱 메모리, 재시작 시 초기화 허용)

| 명령어 | 동작 |
|---|---|
| `/add <종목명 또는 코드>` | 관심종목 즉시 등록 |
| `/add` | 안내 메시지 후 다음 입력으로 등록 |
| `/del <종목명 또는 코드>` | 관심종목 즉시 삭제 |
| `/del` | 안내 메시지 후 다음 입력으로 삭제 |
| `/list` 또는 `/l` | 현재 관심종목 목록 조회 |

- 입력 검증: 6자리 숫자 → 종목코드 조회, 그 외 → 종목명 완전일치 조회 (`StockMaster`)
- `StockMaster` 종목명은 공백 제거 후 저장되므로 입력값 공백 제거만으로 매칭 가능
