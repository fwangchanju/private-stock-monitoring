# PSMS plan.md

---

## 프로젝트 목표

키움증권 REST API를 활용해 HTS의 핵심 표 7개를 웹 대시보드로 재구성.
장중 주기적 수집 → 웹 출력 → 텔레그램 이미지 발송이 핵심 사이클.

---

## 전체 현황 요약

| # | 대시보드 | 상태 | 데이터 소스 |
|---|---|---|---|
| 1 | 시장종합 | ✅ 구현 완료 | ka20001 (5분 주기) |
| 2 | 투자자별 매매종합 | ✅ 구현 완료 | ka10051 (5분 주기, 13종 투자자) |
| 3 | 장중 투자자별 매매 상위 | ✅ 구현 완료 | ka10065 (5분 주기) |
| 4 | 프로그램 순매수 상위 | ✅ 구현 완료 | ka90003 (5분 주기) |
| 5 | 종목별 프로그램매매추이 | ✅ 구현 완료 | ka90008(5분) + ka90013(16시 1회) |
| 6 | 종목별 공매도추이 | ✅ 구현 완료 | KRX 크롤링 MDCSTAT30101 (19시 1회) |
| 7 | 지수기여도 상위 | ✅ 구현 완료 | KRX 크롤링 MDCSTAT01501 (5분 주기 연산) |

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

### 2. 투자자별 매매종합 ✅

| 항목 | 내용 |
|---|---|
| API | ka10051 — 업종별투자자순매수요청 (`/api/dostk/sect`) |
| 저장 전략 | latest (시장×투자자 조합별 1건 upsert) |
| 수집 주기 | 1시간 (`0 0 9-15 * * MON-FRI`) |

- `inds_cd=001` (코스피 종합) / `inds_cd=101` (코스닥 종합) — `stex_tp=1` (KRX only)
- 응답 래퍼: `inds_netprps[]`

| 필드 | 투자자 | InvestorType |
|---|---|---|
| `ind_netprps` | 개인 | PERSONAL |
| `frgnr_netprps` | 외국인 | FOREIGNER |
| `orgn_netprps` | 기관계 | INSTITUTION |
| `sc_netprps` | 금융투자 | FINANCIAL_INVESTMENT |
| `invtrt_netprps` | 투신 | TRUST |
| `endw_netprps` | 연기금 | PENSION_FUND |
| `samo_fund_netprps` | 사모펀드 | PRIVATE_FUND |
| `insrnc_netprps` | 보험 | INSURANCE |
| `bank_netprps` | 은행 | BANK |
| `etc_corp_netprps` | 기타법인 | OTHER_CORP |
| `natn_netprps` | 국가 | GOVERNMENT |
| `jnsinkm_netprps` | 종금 | OTHER_FINANCE |
| `native_trmt_frgnr_netprps` | 국내처리외국인 | FOREIGN_COMPANY |

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

### 6. 종목별 공매도추이 ✅

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

### 7. 지수기여도 상위 ✅

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
| 현재가, 상장주식수, 시가총액 | KRX 전종목 시세 크롤링 | ✅ |
| 전일종가 (현재가 - 등락액으로 역산) | KRX MDCSTAT01501 `CMPPREVDD_PRC` | ✅ |
| 전일 지수값 | MarketOverview (`indexValue - changeValue`) | ✅ |

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

**NotificationScheduler (텍스트 링크 리마인더) 제거 완료**
- 이미지 자동 발송 사이클로 대체 예정 (`psms-screenshot` 컨테이너 미구현)

---

## 미결 / 구현 대기 항목

### ① 접근 제어 (개인용 서비스 보호)

KRX 수집 데이터 재배포 불가.

- **텔레그램 봇**: `TELEGRAM_CHAT_ID` + `DEVELOPER_CHAT_ID` 두 값으로 허용 여부 체크
- **웹 접근 제어**: 허용 IP를 DB 테이블로 관리, 앱 기동 시 메모리 로드 → Nginx 또는 Spring Security 필터에서 차단
  - 세부 구현 방식 미결정 (Nginx conf 재생성 vs Spring Security IP 필터 vs 두 레이어 조합)

### ② 텔레그램 이미지 자동 발송 (`psms-screenshot`)

**핵심 사이클**: 매 수집 완료 → 대시보드 캡처 → 텔레그램 이미지 발송

```
매 정시 (0 0 9-15 * * MON-FRI)
  1. Spring Boot: 데이터 수집 완료
  2. Spring Boot → psms-screenshot 서비스에 HTTP 캡처 요청
  3. psms-screenshot: Puppeteer로 대시보드 headless 렌더링 → 이미지 반환
  4. Spring Boot: 이미지를 텔레그램 sendPhoto로 TELEGRAM_CHAT_ID에 발송
```

**psms-screenshot 서비스** (미구현)
- 별도 Node.js + Express 컨테이너
- Puppeteer로 대시보드 URL headless 렌더링 후 스크린샷 반환
- 기존 `backend` Docker 네트워크에 합류

