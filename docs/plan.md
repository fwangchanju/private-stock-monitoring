# PSMS plan.md

---

## 프로젝트 목표

키움증권 REST API를 활용해 HTS의 핵심 표 7개를 웹 대시보드로 재구성. 정시 체크용 개인 모니터링 도구.

---

## 대시보드 구성 및 구현 계획

### 1. 시장종합

| 항목 | 내용 |
|---|---|
| API | ka20001 — 업종현재가요청 (`/api/dostk/sect`) |
| 저장 전략 | latest |
| 수집 주기 | `0 */5 9-15 * * MON-FRI` |

**수집 로직**
1. 코스피(`inds_cd=001`) / 코스닥(`inds_cd=101`) 각 1회 호출
   - `mrkt_div_tp` 파라미터로 KRX+NXT 통합 단일 호출 가능
2. root-level 필드에서 현재가, 등락률, 거래량, 거래대금, 상승/보합/하락 종목수 추출
3. MarketOverview 엔티티 upsert (latest 1건 유지)

**응답 필드**
- root: `cur_prc`, `pred_pre_sig`, `pred_pre`, `flu_rt`, `trde_qty`, `trde_prica`, `trde_frmatn_stk_num`, `open_pric`, `high_pric`, `low_pric`, `upl`, `rising`, `stdns`, `fall`, `lst`, `52wk_hgst_pric`, `52wk_hgst_pric_dt`, `52wk_lwst_pric`, `52wk_lwst_pric_dt`
- 시계열 배열: `inds_cur_prc_tm[]` — `tm_n`, `cur_prc_n`, `pred_pre_n`, `flu_rt_n`, `trde_qty_n`, `acc_trde_qty_n` (내림차순)

---

### 2. 투자자별 매매종합

| 항목 | 내용 |
|---|---|
| API | ka10051 — 업종별투자자순매수요청 (`/api/dostk/sect`) |
| 저장 전략 | latest |
| 수집 주기 | `0 */5 9-15 * * MON-FRI` |

**수집 로직**
1. 코스피(`mrkt_div_tp=EK`) / 코스닥(`mrkt_div_tp=NK`) 각 1회 호출 (KRX+NXT 통합 단일 호출)
2. 응답 `inds_netprps[]` 에서 종합지수 항목 필터링
   - 코스피: `inds_cd=001` / 코스닥: `inds_cd=101` / 통합 호출 시: `inds_cd=001_AL`
3. 투자자별 순매수 금액 저장
   - `ind_netprps`(개인), `frgnr_netprps`(외국인), `orgn_netprps`(기관계), `sc_netprps`(금융투자), `insrnc_netprps`(보험), `invtrt_netprps`(투신), `bank_netprps`(은행), `endw_netprps`(기금), `samo_fund_netprps`(사모펀드)
4. InvestorTradingSummary 엔티티 upsert (latest 1건 유지)

**제약**: 순매수 금액만 제공. 매수/매도 분리 없음 → 화면에서 순매수 금액만 표시

**응답 필드**: `inds_cd`, `inds_nm`, `cur_prc`, `pre_smbol`, `pred_pre`, `flu_rt`, `trde_qty` + 투자자별 netprps 필드들

---

### 3. 장중 투자자별 매매 상위

| 항목 | 내용 |
|---|---|
| API | ka10065 — 장중투자자별매매상위요청 (`/api/dostk/rkinfo`) |
| 저장 전략 | snapshot |
| 수집 주기 | `0 30 18 * * MON-FRI` (당일 자료 18:30 이후 제공) |

**수집 로직**
1. 코스피(`mrkt_tp=00`) + 코스닥(`mrkt_tp=10`) × 외국인(`orgn_tp=9000`) + 기관계(`orgn_tp=9999`) = **4회 호출**
   - 금액(`amt_qty_tp=1`) 기준
   - **개인 코드 없음** — ka10065는 기관/외국인 매매상위만 제공
2. 응답 배열에서 상위 N개 종목 추출
3. IntradayInvestorRankingSnapshot 엔티티 저장 (snapshot_time = 18:30:00)

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

**미확인**: 실제 응답 필드명 테스트 필요 (`docs/test/ka10065.md` 미작성)

---

### 4. 프로그램 순매수 상위

| 항목 | 내용 |
|---|---|
| API | ka90003 — 프로그램순매수상위50요청 (`/api/dostk/stkinfo`) |
| 저장 전략 | snapshot |
| 수집 주기 | `0 */5 9-15 * * MON-FRI` |

**수집 로직**
1. 코스피(`mrkt_cd=P00101`) + 코스닥(`mrkt_cd=P10102`) × 순매수(`trde_tp=1`)/순매도(`trde_tp=2`) × 금액(`amt_qty_tp=1`)/수량(`amt_qty_tp=2`) = **8회 호출**
2. `stk_cd` 응답값에서 `_AL` suffix 제거 (`000660_AL` → `000660`)
3. 각 조합별로 ProgramTradingRankingSnapshot 저장 (시장구분 + 매매구분 + 금액수량구분 포함)
4. "전체" 탭은 별도 API 호출 없이 QueryService에서 코스피+코스닥 결과를 합산·재정렬하여 제공

**주의**: API 문서 Response 표와 실제 JSON 응답의 필드명이 다름 → 실제 응답 기준 사용

**응답 필드** (래퍼: `prm_netprps_upper_50[]`)
- `rank`, `stk_cd`, `stk_nm`, `cur_prc`, `flu_sig`, `pred_pre`, `flu_rt`, `acc_trde_qty`, `prm_sell_amt`, `prm_buy_amt`, `prm_netprps_amt`

---

### 5. 종목별 프로그램매매추이

| 항목 | 내용 |
|---|---|
| API | ka90008 (시간별) + ka90013 (일별) |
| 저장 전략 | history — 테이블 분리 |
| 수집 주기 | 시간별: `0 */5 9-15 * * MON-FRI` / 일별: `0 0 16 * * MON-FRI` |

**테이블 구분**

| 테이블 | API | 데이터 단위 | 보존 기간 |
|---|---|---|---|
| `program_trading_intraday` | ka90008 | 분 단위 tm | 단기 (수일) |
| `program_trading_daily` | ka90013 | 일자(dt) | 장기 (수십 일) |

**수집 로직 — 시간별 (ka90008)** `/api/dostk/mrkcond`
1. 관심종목(WatchStock) 목록 조회
2. 종목별 KRX(`stk_cd=005930`) + NXT(`stk_cd=005930_NX`) 각 1회 호출
3. **분 단위 버킷팅**: `tm=143433` → `143400` (초 단위 내림)
4. 동일 `(stk_cd, dt, tm_minute)` 키로 HashMap merge — KRX+NXT 금액/수량 합산 (O(n))
5. 두 호출 모두 성공 시에만 저장. 하나라도 실패 시 해당 사이클 스킵 + 로그
6. `program_trading_intraday` upsert

**응답 필드** (래퍼: `stk_tm_prm_trde_trnsn[]`, 내림차순)
- `tm`, `cur_prc`, `pre_sig`, `pred_pre`, `flu_rt`, `trde_qty`, `prm_sell_amt`, `prm_buy_amt`, `prm_netprps_amt`, `prm_netprps_amt_irds`, `prm_sell_qty`, `prm_buy_qty`, `prm_netprps_qty`, `prm_netprps_qty_irds`, `stex_tp`
- **주의**: `prm_netprps_amt` 등이 `--180311` 형태 (부호 prefix + 숫자). 파싱 처리 필요.

**수집 로직 — 일별 (ka90013)** `/api/dostk/mrkcond` (추정, 테스트 필요)
1. 관심종목별 KRX + NXT 각 1회 호출 (동일한 `_NX` suffix 방식)
2. `dt` 기준 HashMap merge — KRX+NXT 합산
3. `program_trading_daily` upsert

**미확인**: ka90013 실제 응답 필드명 테스트 필요 (`docs/test/ka90013.md` 미작성)

---

### 6. 종목별 공매도추이

| 항목 | 내용 |
|---|---|
| API | ka10014 — 공매도추이요청 (`/api/dostk/shsa`) |
| 저장 전략 | history |
| 수집 주기 | `0 0 16 * * MON-FRI` |

**수집 로직**
1. 관심종목(WatchStock) 목록 조회
2. 종목별 KRX(`stk_cd=005930`) + NXT(`stk_cd=005930_NX`) 각 1회 호출
3. `dt` 기준 HashMap merge — KRX+NXT 수량/금액 합산
4. 두 호출 모두 성공 시에만 저장. 하나라도 실패 시 해당 종목 스킵 + 로그
5. ShortSellingHistory upsert (동일 `(stockCode, tradeDate)` 중복 방어)

**응답 필드** (래퍼: `shrts_trnsn[]`, 내림차순)
- `dt`, `close_pric`, `pred_pre_sig`, `pred_pre`, `flu_rt`, `trde_qty`, `shrts_qty`, `ovr_shrts_qty`, `trde_wght`, `shrts_trde_prica`, `shrts_avg_pric`

---

### 7. 지수기여도 상위

**보류**: 키움 REST API에 전용 API 없음 확정. 나머지 6개 완성 후 재검토.
화면은 "데이터 준비 중" 상태로 임시 처리.

---

## Kiwoom API 연동 레이어 재설계

### 변경 내용

**TokenResponse**

| 항목 | 기존 | 수정 후 |
|---|---|---|
| 토큰 필드명 | `accessToken` | `token` |
| 만료 필드명 | `expires_in` | `expires_dt` |
| 만료 타입 | `long` | `String` (yyyyMMddHHmmss) |

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

## 현재 코드 수정 필요 사항

### 즉시 처리

- **IndexContributionRankingCollector**: 추정값 경로로 잘못된 API 호출 중 → CollectionScheduler에서 즉시 제거
- **IntradayInvestorRankingCollector 스케줄러**: 장중 5분 배치 → 18:30 배치로 이동
- **orgn_tp 코드 수정**: `PERSONAL("8000")` 제거, `INSTITUTION("1000")` → `("9999")`로 변경

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
| ka10065 실제 응답 확인 | 테스트 필요 |
| ka90013 실제 응답 확인 | 테스트 필요 |
| ka90008 `prm_netprps_amt` 이중 부호 파싱 | DTO 작업 시 처리 방식 결정 필요 |
| 지수기여도상위 API | 전용 API 없음, 보류 |
| 수집 주기 최종값 | 초기 5분, 운영 후 조정 |
