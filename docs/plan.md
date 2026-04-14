# PSMS plan.md

---

## 프로젝트 목표

키움증권 REST API를 활용해 HTS의 핵심 표 7개를 웹 대시보드로 재구성.
장중 주기적 수집 → 웹 출력 → 텔레그램 이미지 발송이 핵심 사이클.

---

## 전체 현황 요약

| # | 대시보드 | 상태 | 데이터 소스 |
|---|---|---|---|
| 1 | 시장종합 | ✅ 구현 완료 | ka20001 (1시간 주기) |
| 2 | 투자자별 매매종합 | 🔧 추가 구현 필요 | ka10051 (1시간 주기, 일부 필드 미파싱) |
| 3 | 장중 투자자별 매매 상위 | ✅ 구현 완료 | ka10065 (1시간 주기) |
| 4 | 프로그램 순매수 상위 | ✅ 구현 완료 | ka90003 (1시간 주기) |
| 5 | 종목별 프로그램매매추이 | ✅ 구현 완료 | ka90008(1시간) + ka90013(16시 1회) |
| 6 | 종목별 공매도추이 | 🔧 구현 필요 | KRX 크롤링 (장 마감 후 1회) |
| 7 | 지수기여도 상위 | 🔧 구현 필요 | KRX 크롤링 (5분 주기 연산) |

---

## 대시보드 상세 스펙

### 1. 시장종합 ✅

| 항목 | 내용 |
|---|---|
| API | ka20001 — 업종현재가요청 (`/api/dostk/sect`) |
| 저장 전략 | latest (시장별 1건 upsert) |
| 수집 주기 | 1시간 (`0 0 9-15 * * MON-FRI`) |

수집 로직: 코스피(`inds_cd=001`) / 코스닥(`inds_cd=101`) 각 1회 호출. 상승·하락·보합·상한·하한 카운트 포함.

응답 필드: `cur_prc`, `pred_pre`, `flu_rt`, `trde_prica`, `upl`, `rising`, `stdns`, `fall`, `lst` 외 다수

---

### 2. 투자자별 매매종합 🔧

| 항목 | 내용 |
|---|---|
| API | ka10051 — 업종별투자자순매수요청 (`/api/dostk/sect`) |
| 저장 전략 | latest (시장×투자자 조합별 1건 upsert) |
| 수집 주기 | 1시간 (`0 0 9-15 * * MON-FRI`) |

**ka10051 응답 확인 완료** (실제 호출 검증)

- `inds_cd=001` (코스피 종합) / `inds_cd=101` (코스닥 종합) — `stex_tp=1` (KRX only) 호출 시 `_AL` 없이 반환 → 현재 코드 매칭 정상 동작
- 응답 래퍼: `inds_netprps[]`

| 필드 | 투자자 | 수집 여부 |
|---|---|---|
| `ind_netprps` | 개인 | ✅ |
| `frgnr_netprps` | 외국인 | ✅ |
| `orgn_netprps` | 기관계 | ✅ |
| `sc_netprps` | 금융투자 | ❌ 추가 필요 |
| `invtrt_netprps` | 투신 | ❌ 추가 필요 |
| `endw_netprps` | 연기금 | ❌ 추가 필요 |
| `samo_fund_netprps` | 사모펀드 | ❌ 추가 필요 |
| `insrnc_netprps` | 보험 | ❌ 추가 필요 |
| `bank_netprps` | 은행 | ❌ 추가 필요 |
| `etc_corp_netprps` | 기타법인 | ❌ 추가 필요 |
| `natn_netprps` | 국가 | ❌ 추가 필요 |
| `jnsinkm_netprps` | 미확인 (전신금 계열 추정) | ❌ 매핑 확인 필요 |
| `native_trmt_frgnr_netprps` | 국내처리외국인 | ❌ 추가 필요 |

**구현 필요 작업**
1. `InvestorType` enum에 보험·은행·기타법인·국가 추가
2. `InvestorTradingSummaryCollector`에서 미수집 필드 파싱 추가
3. `jnsinkm_netprps` 한국거래소 투자자 분류표 기준 매핑 확정
4. QueryService / DTO / API 엔드포인트 업데이트

---

### 3. 장중 투자자별 매매 상위 ✅

| 항목 | 내용 |
|---|---|
| API | ka10065 — 장중투자자별매매상위요청 (`/api/dostk/rkinfo`) |
| 저장 전략 | snapshot (5분마다 누적) |
| 수집 주기 | 1시간 (`0 0 9-15 * * MON-FRI`) |

수집 로직: 코스피+코스닥 × orgn_tp 11종 × 순매수/순매도 × 금액/수량 = 88회/사이클

orgn_tp 코드: 9000(외국인), 9999(기관계), 1000(금융투자), 2000(보험), 3000(투신), 4000(은행), 5000(기타금융), 6000(연기금), 7000(국가), 7100(기타법인), 9100(외국계)

응답 래퍼: `opmr_invsr_trde_upper[]` — `stk_cd`, `stk_nm`, `sel_qty`, `buy_qty`, `netslmt`

---

### 4. 프로그램 순매수 상위 ✅

| 항목 | 내용 |
|---|---|
| API | ka90003 — 프로그램순매수상위50요청 (`/api/dostk/stkinfo`) |
| 저장 전략 | snapshot (5분마다 누적) |
| 수집 주기 | 1시간 (`0 0 9-15 * * MON-FRI`) |

수집 로직: 코스피+코스닥 × 순매수/차익/비차익 × 금액/수량 = 8회/사이클. `stk_cd`의 `_AL` suffix 제거 후 저장. "전체" 탭은 QueryService에서 합산·재정렬 제공.

응답 래퍼: `prm_netprps_upper_50[]` — `rank`, `stk_cd`, `stk_nm`, `cur_prc`, `flu_rt`, `prm_sell_amt`, `prm_buy_amt`, `prm_netprps_amt`

---

### 5. 종목별 프로그램매매추이 ✅

| 항목 | 내용 |
|---|---|
| API | ka90008 (시간별) + ka90013 (일별) |
| 저장 전략 | history (분봉: `program_trading_history` / 일별: `program_trading_daily`) |
| 수집 주기 | 시간별: 1시간 / 일별: 16:00 1회 |

수집 로직:
- 관심종목(WatchStock) 기준 종목별 KRX + NXT 각 1회 호출
- 분봉: `tm` 필드 분 단위 버킷팅 (`143433 → 143400`), KRX+NXT 합산
- `prm_netprps_amt` 등 `--180311` 형태 이중 부호 파싱 처리
- 운영 후 검증 필요: intraday 누적값과 daily 값 의미 일치 여부

---

### 6. 종목별 공매도추이 🔧

| 항목 | 내용 |
|---|---|
| 데이터 소스 | data.krx.co.kr 크롤링 (개별종목 공매도 거래) |
| 저장 전략 | history — upsert (종목 × 날짜) |
| 수집 주기 | 장 마감 후 1회 (`0 0 19 * * MON-FRI`) |

**ka10014 → KRX 크롤링으로 전환 결정**
- 이유: ka10014는 종목당 2회(KRX+NXT) 호출 → 관심종목 수 증가 시 rate limit 위험
- KRX 크롤링은 전종목 데이터를 1회 호출로 수집 가능

공매도 데이터는 장 마감 후 집계 확정. 장중 실시간 공매도 수집 불가.

**KRX 크롤링 파라미터 (Chrome 직접 확인 완료)**

```
POST data.krx.co.kr/comm/bldAttendant/getJsonData.cmd
bld=dbms/MDC/STAT/srt/MDCSTAT30101
searchType=2         (단일 날짜 조회)
mktId=STK            (코스피) 또는 KSQ (코스닥)
inqCond=STMFRTSCIFDRFSSRSWBC
trdDd=YYYYMMDD
isuCd=               (빈 값 → 전종목 반환, 약 1000건/시장)
share=1
money=1
```

**응답 필드 (OutBlock_1)**

| 필드 | 설명 |
|---|---|
| `ISU_CD` | 종목코드 |
| `ISU_ABBRV` | 종목명 |
| `SECUGRP_NM` | 증권그룹명 |
| `CVSRTSELL_TRDVOL` | 공매도 거래량 |
| `ACC_TRDVOL` | 전체 거래량 |
| `TRDVOL_WT` | 공매도 비중(거래량, %) |
| `CVSRTSELL_TRDVAL` | 공매도 거래대금 |
| `ACC_TRDVAL` | 전체 거래대금 |
| `TRDVAL_WT` | 공매도 비중(거래대금, %) |

**수집 범위**: 전종목 수집 후 관심종목(WatchStock)만 저장. 2회 호출(코스피+코스닥)으로 당일 전종목 커버.

---

### 7. 지수기여도 상위 🔧

| 항목 | 내용 |
|---|---|
| 데이터 소스 | data.krx.co.kr 크롤링 (전종목 시세) |
| 저장 전략 | snapshot (5분마다 연산 후 저장) |
| 수집 주기 | 장 시작 전 전일종가 1회 + 장중 5분 주기 |

**연산 공식**
```
종목 기여도 ≈ (현재가 - 전일종가) × 상장주식수 / 전일 전체 시가총액 × 전일 지수값
```

**수집 구조**
1. 장 시작 전 1회: 전일종가 수집·저장 (당일 내내 고정값으로 사용)
2. 장중 5분 주기: KRX 전종목 현재가·시가총액 수집 → 기여도 연산 → `IndexContributionRankingSnapshot` 저장

| 필요 데이터 | 출처 | 상태 |
|---|---|---|
| 현재가, 상장주식수, 시가총액 | KRX 전종목 시세 크롤링 | 🔧 구현 필요 |
| 전일종가 | KRX 전종목 시세 (장 시작 전 1회) | 🔧 구현 필요 |
| 전일 지수값 | ka20001 (`cur_prc - pred_pre`) | ✅ 이미 수집 중 |

**전용 API 없음 확정**: 키움 REST API 528페이지 전량 분석, 공공데이터포털 모두 해당 없음. KRX 크롤링이 유일한 실시간 소스.

**KRX 크롤링 파라미터 (Chrome 직접 확인 완료)**

```
POST data.krx.co.kr/comm/bldAttendant/getJsonData.cmd
bld=dbms/MDC/STAT/standard/MDCSTAT01501
mktId=STK            (코스피) 또는 KSQ (코스닥) 또는 ALL
trdDd=YYYYMMDD
share=1
money=1
```

**응답 필드 (OutBlock_1)**

| 필드 | 설명 |
|---|---|
| `ISU_SRT_CD` | 종목코드 (단축) |
| `ISU_CD` | ISIN |
| `ISU_ABBRV` | 종목명 |
| `TDD_CLSPRC` | 현재가(당일 종가) |
| `CMPPREVDD_PRC` | 전일대비 등락액 |
| `LIST_SHRS` | 상장주식수 |
| `MKTCAP` | 시가총액 |
| `MKT_ID` | 시장구분 (STK/KSQ) |

---

## 수집 스케줄 원칙

| 패턴 | 용도 |
|---|---|
| `0 0 9-15 * * MON-FRI` | 장중 1시간 주기 수집 (서버 안정화 전까지) |
| `0 0 7 * * MON-FRI` | 종목 마스터 동기화 |
| `0 0 16 * * MON-FRI` | 프로그램매매 일별 이력 |
| `0 0 19 * * MON-FRI` | 공매도 (KRX 크롤링, MDCSTAT30101) |

- 타임존: `Asia/Seoul` 명시
- `snapshotTime`: 현재 시각을 5분 단위 내림 처리한 논리적 기준 시각

---

## 데이터 소스 전략

### 키움 REST API (주력)
- 장중 실시간 데이터 전용
- 호출 간 `callIntervalMs` sleep 적용 (rate limit 대응)
- Request/Response 타입 안전 DTO 패턴 적용 (`KiwoomRequest` 인터페이스)

### KRX 크롤링 (보조 — #6, #7)
- URL: `data.krx.co.kr/comm/bldAttendant/getJsonData.cmd` (POST)
- `bld` 파라미터로 데이터 종류 구분, User-Agent 헤더 필요
- 비공식 인터페이스 — 사이트 구조 변경 시 즉시 영향 받음
- #6과 #7은 같은 크롤링 모듈 공유 구조로 구현

### 텔레그램 이미지 발송 사이클
핵심 사이클: 매 정시 수집 완료 → 스크린샷 캡처 → 텔레그램 이미지 발송

```
매 정시 (0 0 9-15 * * MON-FRI)
  1. Spring Boot: 데이터 수집 완료
  2. Spring Boot → psms-screenshot 서비스에 HTTP 캡처 요청
  3. psms-screenshot: Puppeteer로 대시보드 headless 렌더링 → 이미지 반환
  4. Spring Boot: 이미지를 텔레그램 sendPhoto로 TELEGRAM_CHAT_ID에 발송
```

**psms-screenshot 서비스**
- 별도 Node.js + Express 컨테이너 (`psms-screenshot`)
- Puppeteer로 대시보드 URL을 headless 렌더링 후 스크린샷 반환
- Spring Boot에서 HTTP로 요청, 이미지 바이트 응답
- 기존 `backend` Docker 네트워크에 합류

**현재 NotificationScheduler (텍스트 링크 리마인더) 제거 예정**
- 이미지 자동 발송 사이클로 대체됨

---

## 미결 / 구현 대기 항목

### ① #2 투자자별 매매종합 (ka10051 파싱 추가)
- `jnsinkm_netprps` 투자자 분류 확정 후 enum 매핑
- 미수집 필드 10개 파싱 추가 → 코드 변경 범위 작음

### ② #6 공매도 (KRX 크롤링 신규)
- POST 파라미터 및 응답 필드 확인 완료 (위 #6 섹션 참고)
- 구현: KRX 크롤링 모듈 + `ShortSellingCollector` 교체
- 전종목 수집 후 WatchStock 필터링 저장

### ③ #7 지수기여도 (KRX 크롤링 신규)
- POST 파라미터 및 응답 필드 확인 완료 (위 #7 섹션 참고)
- `IndexContributionRankingCollector` 구현 교체 (현재 TODO 상태)
- #6과 KRX 크롤링 모듈 공유

### ④ 텔레그램 봇을 통한 관심종목 관리
- #5(프로그램매매추이), #6(공매도추이) 모두 `WatchStock` 기반 수집
- 사용자가 텔레그램 봇에 메시지를 보내 관심종목 등록·삭제·조회
- 구현 위치: `notification/` 패키지 내 텔레그램 polling 수신 로직 추가
- 봇 명령 처리 → `WatchStockRepository` CRUD 연동

**지원 명령어**

| 명령어 | 동작 |
|---|---|
| `/add <종목명 또는 코드>` | 관심종목 즉시 등록 |
| `/add` | 종목명/코드 입력 안내 후 다음 메시지로 등록 |
| `/del <종목명 또는 코드>` | 관심종목 즉시 삭제 |
| `/del` | 종목명/코드 입력 안내 후 다음 메시지로 삭제 |
| `/list` 또는 `/l` | 현재 관심종목 목록 조회 |

**대화 상태 관리**
- chatId별 상태를 앱 메모리(`ConcurrentHashMap<String, ConversationState>`)로 관리
- 재시작 시 상태 초기화 허용 (개인용 봇, 실질적 문제 없음)
- 상태: `IDLE` / `WAITING_FOR_ADD_STOCK` / `WAITING_FOR_DEL_STOCK`

**입력 validation 흐름**
1. 입력값 공백 제거
2. 숫자 6자리 → 종목코드로 `StockMaster` 조회
3. 그 외 → 종목명으로 `StockMaster` 조회 (완전 일치)
4. 조회 결과 없음 → "등록되지 않은 종목입니다" 응답
5. 조회 결과 있음 → `WatchStock` 등록/삭제 후 결과 응답
- validation 소스: `StockMaster` 테이블 (매일 07:00 ka10099로 전종목 갱신)

**StockMaster 적재 규칙**
- 종목명(`name`) 저장 시 공백 제거 후 적재 (`name.replace(" ", "")`)
- 이후 모든 종목명 비교는 입력값 공백 제거만으로 충분

---

### ⑤ 예외 처리 체계 구성

**예외 계층**
```
BusinessException (최상위, RuntimeException 상속)
├── EscalateException (상속) — 크리티컬 구간, 핸들러에서 DEVELOPER_CHAT_ID로 텔레그램 발송
└── 그 외 세부 예외들 — 일반 핸들러 메서드에서 로그/트레이싱만 처리
```

**공통 핸들러**
- `@ExceptionHandler(EscalateException.class)` → 로그 + DEVELOPER_CHAT_ID 텔레그램 발송
- `@ExceptionHandler(BusinessException.class)` → 로그만 (EscalateException 제외 나머지)

**텔레그램 발신 채널 분리**
- `TELEGRAM_CHAT_ID`: 사용자 대상 — 리마인더, 수집 결과 등 정상 알림
- `DEVELOPER_CHAT_ID`: 개발자(운영자) 대상 — EscalateException 발생 시 오류 내용 직접 발송

**EscalateException 상속 대상 (즉시 알림 필요)**
1. 키움 API 토큰 발급·갱신 실패 — 장중 수집 전체가 조용히 멈춤, 화면상 인지 불가
2. KRX 크롤링 응답 구조 변경 감지 — 비공식 인터페이스, #6/#7 수집 통째로 중단
3. 하루 1회 수집 실패 (공매도 19:00, 프로그램매매 일별 16:00) — 다음 기회 없음, 당일 데이터 영구 결손
4. 종목 마스터 동기화 실패 (07:00 ka10099) — 당일 WatchStock 검증·수집 전체에 영향

**BusinessException으로만 처리 (로그만)**
- 1시간 주기 수집 중 특정 종목 1건 파싱 실패 — 다음 사이클에서 재수집
- 텔레그램 봇 명령 입력 오류 — 사용자가 직접 인지
- 중복 등록 등 사용자 조작 오류

- I/O, 하드웨어 등 딥한 예외까지 처리할 필요는 없음

