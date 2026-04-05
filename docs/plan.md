# PSMS plan.md

---

## 프로젝트 목표

키움증권 REST API를 활용해 HTS의 핵심 표 7개를 웹 대시보드로 재구성. 정시 체크용 개인 모니터링 도구.

---

## 대시보드 구성 및 구현 계획

### 1. 시장종합 ✅ 결정 완료

| 항목 | 내용 |
|---|---|
| API | ka20001 — 업종현재가요청 (`/api/dostk/sect`) |
| 저장 전략 | latest |
| 수집 주기 | `0 */5 8-20 * * MON-FRI` (Asia/Seoul) |

**수집 로직**
1. 코스피(`inds_cd=001`) / 코스닥(`inds_cd=101`) 각 1회 호출
2. root-level 필드 추출 후 MarketOverview 엔티티 upsert (latest 1건 유지)

**화면 출력 구조**

| 표시 항목 | 계산 |
|---|---|
| 상승 | `upl + rising` |
| 하락 | `fall + lst` |
| 보합 | `stdns` |
| 상한 | `upl` (별도) |
| 하한 | `lst` (별도) |

DB에는 필드 그대로 저장, QueryService에서 조합하여 제공.

**응답 필드**
- root: `cur_prc`, `pred_pre_sig`, `pred_pre`, `flu_rt`, `trde_qty`, `trde_prica`, `trde_frmatn_stk_num`, `open_pric`, `high_pric`, `low_pric`, `upl`, `rising`, `stdns`, `fall`, `lst`, `52wk_hgst_pric`, `52wk_hgst_pric_dt`, `52wk_lwst_pric`, `52wk_lwst_pric_dt`
- 시계열 배열: `inds_cur_prc_tm[]` — `tm_n`, `cur_prc_n`, `pred_pre_n`, `flu_rt_n`, `trde_qty_n`, `acc_trde_qty_n` (내림차순)

---

### 2. 투자자별 매매종합

**코스피/코스닥 부분 구현 가능. 선물/옵션/국채/달러선물은 보류.**

---

**API 분석 결과 (키움 REST API 문서 528페이지 전량 분석)**

ka10051(업종별투자자순매수요청)은 업종 목록을 반환하는데, 그 중 `inds_cd="001_AL"`(코스피 종합지수), `inds_cd="101_AL"`(코스닥 종합지수) 항목이 시장 전체 투자자별 집계값을 제공한다.

응답 필드 → 화면 B 컬럼 매핑:

| 화면 B 컬럼 | ka10051 응답 필드 | 현재 수집 여부 |
|---|---|---|
| 개인 | `ind_netprps` | ✅ 수집 중 |
| 외국인 | `frgnr_netprps` | ✅ 수집 중 |
| 기관계 | `orgn_netprps` | ✅ 수집 중 |
| 금융투자 | `sc_netprps` | ❌ 미수집 |
| 투신 | `invtrt_netprps` | ❌ 미수집 |
| 연기금등 | `endw_netprps` | ❌ 미수집 |
| 사모펀드 | `samo_fund_netprps` | ❌ 미수집 |

선물/콜옵션/풋옵션/3년국채/10년국채/달러선물 행은 키움 국내주식 REST API 문서에 해당 API 없음. 파생상품 전용 문서가 별도로 존재할 수 있으나 현재 미확보.

---

**구현 범위 결정 (확정 필요)**

- 코스피/코스닥 2행 × 7개 투자자 컬럼 테이블로 축소 구현
- 파생상품 행은 추후 추가 또는 영구 제외

---

**구현 시 필요 작업**

| 순서 | 작업 |
|---|---|
| 1 | `InvestorType` enum에 FINANCIAL_INVESTMENT, TRUST, PENSION_FUND, PRIVATE_FUND 추가 |
| 2 | `InvestorTradingSummaryCollector`에서 4개 투자자 타입 수집 추가 (`sc_netprps`, `invtrt_netprps`, `endw_netprps`, `samo_fund_netprps`) |
| 3 | `findCompositeItem` 매칭 로직 수정 — 현재 `"001"` 완전 일치 → `startsWith("001")` 또는 실제 응답값 확인 후 수정 |
| 4 | Flyway 마이그레이션 불필요 (enum String 컬럼, 새 값 추가만으로 동작) |
| 5 | QueryService/DTO/API 엔드포인트/프론트 테이블 구현 |

---

**사용자 확인 필요 사항**

1. **`inds_cd` 실제 값**: 문서 Response Example에서 `"001_AL"`로 표시됨. 현재 코드는 `"001"`로 완전 일치 비교 → 실제 API 호출 후 응답 확인 필요 (못 찾으면 종합지수 항목을 못 불러옴)
2. **`amt_qty_tp` 파라미터**: 현재 코드에서 `"0"`으로 설정 중. 문서 Description에는 `금액:0, 수량:1`로 보이나 Request Example에도 `"0"` 사용 → 실제 호출 시 정상 응답 여부 확인
3. **코스피/코스닥 행만으로 충분한지** 여부 결정

---

### 3. 장중 투자자별 매매 상위 ✅ 결정 완료

| 항목 | 내용 |
|---|---|
| API | ka10065 — 장중투자자별매매상위요청 (`/api/dostk/rkinfo`) |
| 저장 전략 | snapshot (장중 5분마다 갱신) |
| 수집 주기 | `0 */5 8-20 * * MON-FRI` (Asia/Seoul, 시간외 거래 포함) |

**수집 로직**
1. 코스피(`mrkt_tp=00`) + 코스닥(`mrkt_tp=10`) × orgn_tp 11종 × 순매수(`trde_tp=1`)/순매도(`trde_tp=2`) × 금액(`amt_qty_tp=1`)/수량(`amt_qty_tp=2`) = **88회 호출 / 사이클**
2. 응답 배열에서 상위 N개 종목 추출
3. IntradayInvestorRankingSnapshot 엔티티 저장

**orgn_tp 코드 전체**

| 코드 | 투자자 |
|---|---|
| 9000 | 외국인 |
| 9999 | 기관계 |
| 1000 | 금융투자 |
| 2000 | 보험 |
| 3000 | 투신 |
| 4000 | 은행 |
| 5000 | 기타금융 |
| 6000 | 연기금 |
| 7000 | 국가 |
| 7100 | 기타법인 |
| 9100 | 외국계 |

**응답 필드** (래퍼: `opmr_invsr_trde_upper[]`)
- `stk_cd`, `stk_nm`, `sel_qty`, `buy_qty`, `netslmt`

---

### 4. 프로그램 순매수 상위 ✅ 결정 완료

| 항목 | 내용 |
|---|---|
| API | ka90003 — 프로그램순매수상위50요청 (`/api/dostk/stkinfo`) |
| 저장 전략 | snapshot (장 중 5분마다 갱신) |
| 수집 주기 | `0 */5 8-20 * * MON-FRI` (Asia/Seoul, 시간외 거래 포함) |

**수집 로직**
1. 코스피(`mrkt_cd=P00101`) + 코스닥(`mrkt_cd=P10102`) × 순매수(`trde_tp=1`)/순매도(`trde_tp=2`) × 금액(`amt_qty_tp=1`)/수량(`amt_qty_tp=2`) = **8회 호출**
   - `stex_tp=3` (통합) 파라미터로 KRX+NXT 통합 결과 단일 호출
2. `stk_cd` 응답값에서 `_AL` suffix 제거 (`000660_AL` → `000660`)
3. 각 조합별로 ProgramTradingRankingSnapshot 저장 (시장구분 + 매매구분 + 금액수량구분 포함)
4. "전체" 탭은 별도 API 호출 없이 QueryService에서 코스피+코스닥 결과를 합산·재정렬하여 제공

**응답 필드** (래퍼: `prm_netprps_upper_50[]`)
- `rank`, `stk_cd`, `stk_nm`, `cur_prc`, `flu_sig`, `pred_pre`, `flu_rt`, `acc_trde_qty`, `prm_sell_amt`, `prm_buy_amt`, `prm_netprps_amt`

---

### 5. 종목별 프로그램매매추이 ✅ 결정 완료 (일부 운영 후 검증 필요)

| 항목 | 내용 |
|---|---|
| API | ka90008 (시간별) + ka90013 (일별) |
| 저장 전략 | history — 테이블 분리 |
| 수집 주기 | 시간별: `0 */5 8-20 * * MON-FRI` (Asia/Seoul) / 일별: `0 0 17 * * MON-FRI` (Asia/Seoul) |

**테이블 구분**

| 테이블 | API | 데이터 단위 | 보존 기간 |
|---|---|---|---|
| `program_trading_intraday` | ka90008 | 분 단위 tm | 단기 (수일) |
| `program_trading_daily` | ka90013 | 일자(dt) | 장기 (수십 일) |

**수집 로직 — 시간별 (ka90008)** `/api/dostk/mrkcond`
1. WatchStock 테이블에서 관심종목 목록 조회 (텔레그램 봇으로 등록/삭제/조회)
2. 종목별 KRX(`stk_cd=005930`) + NXT(`stk_cd=005930_NX`) 각 1회 호출
3. **분 단위 버킷팅**: `tm=143433` → `143400` (초 단위 내림)
4. 동일 `(stk_cd, dt, tm_minute)` 키로 HashMap merge — KRX+NXT 금액/수량 합산 (O(n))
5. 1회 재처리 후에도 실패 시 해당 종목 스킵 + 로그
6. `program_trading_intraday` upsert
7. **화면 출력**: 최신 5건, 시간 역순

**응답 필드** (래퍼: `stk_tm_prm_trde_trnsn[]`, 내림차순)
- `tm`, `cur_prc`, `pre_sig`, `pred_pre`, `flu_rt`, `trde_qty`, `prm_sell_amt`, `prm_buy_amt`, `prm_netprps_amt`, `prm_netprps_amt_irds`, `prm_sell_qty`, `prm_buy_qty`, `prm_netprps_qty`, `prm_netprps_qty_irds`, `stex_tp`
- **주의**: `prm_netprps_amt` 등이 `--180311` 형태 (부호 prefix + 숫자). 파싱 처리 필요.

**수집 로직 — 일별 (ka90013)** `/api/dostk/mrkcond`
1. WatchStock 목록 기준 종목별 KRX + NXT 각 1회 호출
2. `dt` 기준 HashMap merge — KRX+NXT 합산
3. `program_trading_daily` upsert

**응답 필드** (래퍼: `stk_daly_prm_trde_trnsn[]`, ka90008와 동일 구조, `tm` → `dt`)
- `dt`, `cur_prc`, `pre_sig`, `pred_pre`, `flu_rt`, `trde_qty`, `prm_sell_amt`, `prm_buy_amt`, `prm_netprps_amt`, `prm_netprps_amt_irds`, `prm_sell_qty`, `prm_buy_qty`, `prm_netprps_qty`, `prm_netprps_qty_irds`, `stex_tp`

**일별 화면 조회 로직** (QueryService)

| 상황 | 데이터 소스 |
|---|---|
| 장 중 (평일 08:00~20:00) | 당일: `intraday` 최신 1건 + 이전 4일: `daily` dt 역순 4건 |
| 시간 외 | `daily` dt 역순 5건 |

- `prm_netprps_amt`가 intraday에서 누적값인지 확인 필요 — ka90013 일별 값과 의미 일치 여부 운영 후 검증

---

### 6. 종목별 공매도추이 ✅ 결정 완료

| 항목 | 내용 |
|---|---|
| API | ka10014 — 공매도추이요청 (`/api/dostk/shsa`) |
| 저장 전략 | history — upsert (최신값으로 교체, 누적 아님) |
| 수집 주기 | `0 0 19 * * MON-FRI` (Asia/Seoul, 당일 자료 18:30 이후 제공) |

**수집 로직**
1. WatchStock 테이블에서 관심종목 목록 조회 (5번과 테이블 공유, 추후 분리 가능성 있음)
2. 종목별 KRX(`stk_cd=005930`) + NXT(`stk_cd=005930_NX`) 각 1회 호출
   - 조회 기간: `LocalDate.now().minusMonths(2)` ~ `LocalDate.now()`
3. `dt` 기준 HashMap merge — KRX+NXT 합산
   - `shrts_qty`, `ovr_shrts_qty`, `trde_qty`, `shrts_trde_prica`: KRX + NXT 합산
   - `shrts_avg_pric`: 합산 후 재계산 (`shrts_trde_prica / shrts_qty`)
   - `close_pric`, `pred_pre`, `flu_rt`: KRX 값 사용
4. 1회 재시도 후에도 실패 시 해당 종목 스킵 + 로그
5. ShortSellingHistory upsert — 동일 `(stockCode, tradeDate)` 존재 시 최신값으로 교체

**응답 필드** (래퍼: `shrts_trnsn[]`, 내림차순)
- `dt`, `close_pric`, `pred_pre_sig`, `pred_pre`, `flu_rt`, `trde_qty`, `shrts_qty`, `ovr_shrts_qty`, `trde_wght`, `shrts_trde_prica`, `shrts_avg_pric`

---

### 7. 지수기여도 상위

**보류**: 키움 국내주식 REST API 문서 528페이지 전량 분석 결과 지수기여도 전용 API 없음 확정. 구현 불가 판정 보류 중.

**사용자 확인 필요 사항**: 키움증권 포털(developers.kiwoom.com)에서 "지수기여도" 관련 API 존재 여부 직접 확인. 존재하지 않으면 이 화면은 제외.

---

## Kiwoom API 연동 레이어 재설계

### 변경 내용

**TokenResponse**

| 항목 | 기존 | 수정 후 |
|---|---|---|
| 토큰 필드명 | `accessToken` | `token` |
| 만료 필드명 | `expires_in` | `expires_dt` |
| 만료 타입 | `long` | `String` (yyyyMMddHHmmss) |

**KiwoomProperties 추가 필드**

```properties
# application-prod.properties
kiwoom.call-interval-ms=100
```

```java
@ConfigurationProperties(prefix = "kiwoom")
public record KiwoomProperties(
    String appKey,
    String secret,
    long callIntervalMs   // API 호출 간 딜레이 (ms). rate limit 대응
) {}
```

`KiwoomApiClient.post()` 내부에서 각 호출 후 `Thread.sleep(callIntervalMs)` 적용.

---

**KiwoomRequest 인터페이스**

```java
public interface KiwoomRequest {
    String path();
    String apiId();
}
```

각 Request DTO가 `path()`, `apiId()`를 직접 보유. 호출부에서 경로를 별도 인자로 넘기지 않음.

**KiwoomResponse\<T\> 공통 래퍼**

```java
public class KiwoomResponse<T> {
    private int returnCode;
    private String returnMsg;
    private T data;
}
```

**KiwoomApiClient.post() 시그니처**

```java
// 변경 전
public JsonNode post(String path, String apiId, Map<String, String> body)

// 변경 후
public <T> KiwoomResponse<T> post(KiwoomRequest request, Class<T> dataClass)
```

### 작업 순서

| 순서 | 작업 |
|---|---|
| 1 | `TokenResponse` 필드명/타입 수정 |
| 2 | `KiwoomResponse<T>` 공통 래퍼 생성 |
| 3 | `KiwoomRequest` 인터페이스 생성 |
| 4 | API별 Response 타입 생성 (`Ka*Data`) |
| 5 | API별 Request DTO 생성 (`Ka*Request`) |
| 6 | `KiwoomApiClient.post()` 제네릭으로 변경 |
| 7 | 각 Collector에서 새 타입 사용하도록 변경 |

---

## 스케줄 공통 원칙

- 타임존: **Asia/Seoul** 명시
- 수집 주기: **5분 간격** (`0 */5 ...`)
- 수집 시간대: **시간외 거래 포함** (`8-20` — NXT 정책 기준 08:00 ~ 20:00 커버)
- 공통 cron 패턴: `0 */5 8-20 * * MON-FRI`
- 단, 종가 기준 1회성 수집(ka10014, ka90013)은 `0 0 16 * * MON-FRI`

---

## 현재 코드 수정 필요 사항

### 즉시 처리

- **IndexContributionRankingCollector**: 추정값 경로로 잘못된 API 호출 중 → CollectionScheduler에서 즉시 제거
- **orgn_tp 코드 수정**: `PERSONAL("8000")` 제거, `INSTITUTION("1000")` → `("9999")`로 변경. 전체 11종 코드로 확장

### DTO 작업과 함께 처리

- **ka90008 순서 의존 제거**: `outputList.get(0)` → `tm` 필드 기준 최신 항목 판별
- **ProgramTradingCollector 코스닥 추가**: 코스피만 수집 → 8회 호출 구조로 변경
- **ProgramTradingHistory 중복 방어**: 동일 `(stockCode, snapshotTime)` 중복 저장 방어 추가

### 신규 구현

- **ka90013 Collector**: 일별 프로그램매매추이 수집 로직 + `program_trading_daily` 테이블 + 16:00 배치

---

## 미결 사항

| 항목 | 상태 |
|---|---|
| ka90008 `prm_netprps_amt` 이중 부호 파싱 | DTO 작업 시 처리 방식 결정 필요 |
| 지수기여도상위 API | 전용 API 없음, 보류 |
| 수집 주기 최종값 | 초기 5분, 운영 후 조정 |


## 외부 API 조사 결과

### 현황 요약 (키움 외 API로 미해결 항목 커버 가능 여부)

| 미해결 항목 | 키움 | KRX OpenAPI | data.go.kr | pykrx | KIS OpenAPI |
|---|---|---|---|---|---|
| 지수기여도상위 | ❌ | ❌ | ❌ | ❌ | 🔲 미확인 |
| 투자자별매매 (선물/옵션) | ❌ | ❌ | ❌ | ❌ | 🔲 미확인 |
| 투자자별매매 (국채/달러선물) | ❌ | ❌ | ❌ | ❌ | 🔲 미확인 |

**data.go.kr (공공데이터포털) 제미나이 주장 검증 결과: 거짓**
- 금융위원회 API가 있으나 주식/지수/ETF 일별 시세 수준. 실시간 아님.
- 투자자별 매매동향, 지수기여도, 프로그램매매 API 없음.
- 공매도 관련은 `금융위원회_주식대차정보` (일별)로 부분 대응 가능하나 키움 ka10014 대체 여부 불확실.
- 7개 대시보드 전체 구현 불가.

**지수기여도 계산 가능 여부**
- 전용 API는 어디에도 없으나 계산으로 도출 가능.
- 공식: `지수기여도 = 전일 KOSPI지수 × 시총비중 × 종목등락률 / 10000`
- 필요 재료: 전일 KOSPI지수(ka20001 수집 중), 종목 등락률(키움 API), 시총비중(pykrx 또는 KRX)
- 단점: pykrx는 스크래핑 기반(불안정), 시총비중은 전일 기준

---

### 주요 증권정보 API 종류

| API | 운영 주체 | 특징 | 비고 |
|---|---|---|---|
| **키움증권 REST API** | 키움증권 | 현재 사용 중. 국내주식 현물 특화. 파생상품 투자자별 없음 | |
| **KIS OpenAPI** | 한국투자증권 | 국내주식/선물옵션/채권/해외 폭넓은 커버리지. 투자자별·랭킹 API 있음. REST + WebSocket | **우선 확인 필요** |
| **KRX Open API** | 한국거래소 | 지수/주식/파생상품 일별 시세. 투자자별 매매동향 없음. 비상업적만 허용 | openapi.krx.co.kr |
| **data.go.kr 금융위원회** | 금융위원회 | 주식/지수/ETF 일별 시세. 실시간 없음. 투자자별/지수기여도/프로그램매매 없음 | |
| **pykrx** | 비공식 오픈소스 | KRX 웹 스크래핑 라이브러리. 투자자별 거래대금(현물만). 지수기여도 없음 | 스크래핑, 불안정 |
| **FinanceDataReader** | 비공식 오픈소스 | 여러 소스 통합. OHLCV·재무 위주. 한국 세부 데이터 제한적 | 스크래핑 |
| **대신증권 Cybos Plus** | 대신증권 | 매우 광범위한 데이터. 단, Windows COM 기반 → 서버 환경 불가 | |
| **이베스트투자증권 xingAPI** | 이베스트 | COM 기반 → 서버 환경 불가 | |
| **야후 파이낸스 (yfinance)** | Yahoo | 글로벌 주식 OHLCV. 한국 투자자별/파생상품 데이터 없음 | |
| **Alpha Vantage** | Alpha Vantage | 글로벌 시세 위주. 한국 파생상품/투자자별 없음 | |

---

### TODO (사용자가 직접 확인 후 재개)

미결 2개 화면은 데이터 소스 확정 후 진행. 나머지 5개 화면 구현과 무관하게 독립적으로 처리 가능.

- [ ] **투자자별 매매동향 — 파생상품 행 (선물/옵션/국채/달러선물)**
  - data.krx.co.kr `MDC0201050302` (파생상품 > 거래실적 > 투자자별 거래실적) 메뉴 존재 확인됨
  - 크롤링 방법: 브라우저 개발자 도구로 해당 페이지 접속 → 네트워크 탭 → `getJsonData.cmd` 요청의 `bld` 파라미터 확인
  - 일별 데이터라는 제약 있음 (실시간 아님)

- [ ] **지수기여도 상위**
  - KRX 전체 메뉴 트리 확인 결과 전용 메뉴 없음
  - KIS OpenAPI 명세 엑셀 (`apiportal.koreainvestment.com`) 에서 지수기여도 API 존재 여부 확인
  - 없을 경우 계산 방식 검토: `전일 KOSPI지수 × 시총비중 × 등락률 / 10000`