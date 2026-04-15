# PSMS (Private Stock Monitoring Service)

키움증권 REST API 기반 개인 주식 모니터링 대시보드. 정시 체크용 웹 대시보드 + 텔레그램 링크 리마인더로 구성.

---

## 대전제 — 서비스의 존재 이유

이 서비스의 핵심 사이클은 아래 3단계다. 기능을 추가하거나 설계를 결정할 때 이 사이클을 기준으로 판단한다.

```
장중 주기적 수집 → 웹 대시보드 출력 → 텔레그램 발송
```

### 수집
- **장중 주기 수집이 전제**다. 수집 주기는 현재 1시간 단위(서버 안정화 전까지), 추후 사용자가 직접 설정 가능하도록 확장 예정.
- **30분 내외의 딜레이는 허용**한다. 일 단위(T+1) 데이터는 이 서비스의 목적에 부합하지 않는다.
- **7개 대시보드는 모두 구현 목표다.** 단, 특정 대시보드의 데이터 소스가 T+1밖에 없는 경우, 해당 대시보드를 포기하는 것이 아니라 장중 실시간에 준하는 다른 소스나 연산 방식을 먼저 모색한다.

### 화면 출력
- 7개 대시보드를 웹 화면에 출력한다.
- **7개 전체를 한 번에 이미지로 캡처하여 발송할 수 있는 버튼**을 제공한다. 사용자가 버튼을 누르면 전체 대시보드가 이미지로 변환되어 텔레그램으로 즉시 발송된다.

### 텔레그램 발송
- 7개 대시보드 이미지를 **텔레그램 봇을 통해 사용자에게 직접 발송**한다.
- 텍스트 링크가 아닌 **이미지 자체를 메시지로 전송**하는 것이 목표다.

---

## 기술 스택

- **Backend**: Spring Boot 4.0.3, Java 21, Gradle (Groovy DSL), JPA, Flyway
- **DB**: MariaDB 10.6
- **Frontend**: React + Vite + TypeScript
- **인프라**: Docker, Nginx, GHCR

---

## 인프라 구성

Oracle Free Tier AMD 서버 2대.

- **1번 서버**: 앱 운영 서버. psmsapp, mariadb, nginx 컨테이너 기동 중.
  - nginx는 `~/infra/nginx/docker-compose.yml`로 별도 관리 (`image: ghcr.io/fwangchanju/psms-nginx:latest`).
  - nginx.conf는 이 레포의 `deploy/nginx/nginx.conf`를 볼륨 마운트.
- **2번 서버**: 빌드 서버. GitHub Actions로 대체됨.

배포 흐름 (전체 자동화 완료):
- `auto-build` 브랜치 push → 빌드(psms + psms-nginx) → GHCR push → SSH로 1번 서버 자동 배포
- GitHub Actions 수동 실행: app / nginx / all 선택 → 해당 이미지 빌드 + 배포
- 워크플로우: `.github/workflows/build-and-push.yml`

### Docker 네트워크

`backend` (external). mariadb와 psmsapp이 같은 네트워크.

### 환경변수 파일

1번 서버 `~/env/private-stock-monitoring.env`. **비밀값만** 관리:
- `DB_APP_PASSWD`, `DB_ADM_PASSWD`
- `KIWOOM_APP_KEY`, `KIWOOM_SECRET`
- `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`, `DEVELOPER_CHAT_ID`

비밀값이 아닌 설정(host, port, username 등)은 `application-prod.properties`에 하드코딩.

### DB 계정 분리

- `psmsadm`: Flyway 마이그레이션 전용 (DDL 권한)
- `psmsapp`: 앱 런타임 전용 (DML 권한만)

---

## 패키지 구조

```
dev.eolmae.psms
├── api/               # 조회 API (Controller, QueryService, DTO)
├── domain/
│   ├── common/        # enum (MarketType, InvestorType 등)
│   ├── user/          # AppUser, UserNotificationSetting
│   ├── stock/         # StockMaster, WatchStock
│   ├── dashboard/     # MarketOverview, InvestorTradingSummary, snapshot 류
│   └── history/       # ProgramTradingHistory, ShortSellingHistory
├── exception/         # BusinessException, EscalateException, GlobalExceptionHandler
├── external/
│   ├── kiwoom/        # KiwoomApiClient, KiwoomTokenManager, DTO 류
│   └── krx/           # KrxCrawler (KRX 데이터 포털 크롤링)
├── collector/         # 수집 스케줄러 + 7개 Collector
└── notification/      # TelegramClient, AlertService, TelegramPollingService, WatchStockBotHandler
```

---

## 구현 현황

**완료**
- 도메인 레이어 전체 (Entity, Repository)
- 조회 API / QueryService / DTO (빈 데이터 처리, 랭킹 상세 엔드포인트 포함)
- Flyway 마이그레이션 V1 (스키마), V2 (샘플 데이터)
- 1번 서버에서 prod 프로파일로 앱 정상 기동
- 예외 처리 체계: `BusinessException` / `EscalateException` / `GlobalExceptionHandler` / `AlertService`
- 키움 API 연동: KiwoomProperties, KiwoomTokenManager, KiwoomApiClient, KiwoomResponseParser, Request/Response DTO
- KRX 크롤링: `KrxCrawler` — `data.krx.co.kr` POST 크롤러, OutBlock_1 파싱
- 수집기 전체 (collector/): 7개 Collector + CollectionScheduler

| 수집기 | 소스 | TR ID / bld |
|---|---|---|
| `MarketOverviewCollector` | 키움 | ka20001 — `/api/dostk/sect` |
| `InvestorTradingSummaryCollector` | 키움 | ka10051 — `/api/dostk/sect` (13종 투자자) |
| `IntradayInvestorRankingCollector` | 키움 | ka10065 — `/api/dostk/rkinfo` |
| `ProgramTradingRankingCollector` | 키움 | ka90003 — `/api/dostk/stkinfo` |
| `ProgramTradingCollector` | 키움 | ka90008 (장중) / ka90013 (일별) |
| `ShortSellingCollector` | KRX | MDCSTAT30101 (전종목 → WatchStock 필터) |
| `IndexContributionRankingCollector` | KRX | MDCSTAT01501 (시가총액 기반 기여도 직접 연산) |
| `StockMasterCollector` | 키움 | ka10099 — `/api/dostk/stkinfo` |

- 텔레그램 봇: `TelegramPollingService` + `WatchStockBotHandler` — `/add` `/del` `/list` 관심종목 관리
- 프론트엔드: React + Vite + TypeScript 전체 구현
  - 대시보드 페이지 (6개 섹션)
  - 상세 페이지 4종 (장중랭킹, 프로그램매매, 지수기여도, 종목상세)
- Nginx 이미지 빌드 통합 (`deploy/nginx/Dockerfile`: 프론트 빌드 → nginx)
- dashboard.base-url: `https://eolmae.duckdns.org`
- 배포 스크립트 분리: `deploy-app.sh` (앱만), `deploy-nginx.sh` (nginx만), `deploy.sh` (통합)
- GitHub Actions CI/CD 파이프라인 완성 (빌드 → GHCR push → SSH 자동 배포까지 완전 자동화)

**미완료**
- 텔레그램 이미지 자동 발송: `psms-screenshot` 컨테이너(Node.js + Puppeteer) 미구현, 대시보드 버튼 → 이미지 캡처 → 텔레그램 sendPhoto 흐름 미구현


---

## 핵심 설계 원칙

- 웹 요청 시 외부 API 직접 호출 금지. 수집 → 저장 → 조회 흐름 엄수.
- 저장 전략: `latest` (시장종합, 투자자매매종합) / `snapshot` (랭킹류) / `history` (종목별 추이)
- `snapshot_time`: 논리적 기준 시각 / `created_at`: 실제 저장 시각 — 둘을 항상 구분.
- 백엔드가 화면에 필요한 형태로 가공해서 제공. 프론트는 렌더링만.
- 접근 제어: Nginx Basic Auth 수준 (정식 인증 체계 없음).
- HTTPS: Duck DNS + Certbot (Let's Encrypt) 적용.

---

## 작업 원칙

- 커밋 메시지: 한글. 고유명사(라이브러리명, 기술명)는 영어 허용.
- 커밋 단위: 성격이 명확히 다른 변경은 분리. 단, 사소한 변경까지 미세하게 쪼개지 않음.
- Push는 사용자가 직접 수행. Claude는 커밋까지만.
- 작업은 한 번에 몰아서 진행하지 않는다. 단계별로 확인하며 진행하고, 각 단계에서 사용자의 확인을 거친다.
- 기능 추가·변경 시 `docs/features.md`를 함께 현행화한다. 수집 방식, 저장 전략, API 경로, 스케줄 중 달라진 항목이 있으면 반드시 반영한다.

---

## 코드 구현 원칙

스프링 철학에 맞는 구현. 10년도 더 된 코드 스타일은 지양. 완전 최신 코드 스타일을 바라는것은 아님, 적당히 최신 스타일을 원함.

- `Map`, `JsonNode` 등으로 타입을 뭉개는 방식 대신 명확한 DTO/Record를 사용한다.
- `ResponseEntity`를 불필요하게 남용하지 않는다. 현재 Spring 생태계의 흐름은 컨트롤러가 도메인 객체나 DTO를 직접 반환하는 방식을 선호한다.
- CustomException 구성(RuntimeException 상속, 이름은 BusinessException 정도면 될 듯), 공통 예외 핸들러를 만들어 BusinessException 에 대한 처리 및 오류 로그 출력(ResponseStatus 사용)
- 각 대시보드 생성 중 발생가능한 Exception 에 해당하는 CustomException 구성 (BusinessException 상속), I/O Exception 이나 기타 하드웨어 혹은 딥한 예외까지 처리할 필요는 없음
- 빠른 구현보다 읽는 사람이 의도를 바로 파악할 수 있는 코드를 우선한다.

---

## 테스트 원칙

로컬에 Docker가 없고 서버에 JDK가 없는 환경 제약상, 테스트는 **서버에서 Docker로 실행**하는 방식을 사용한다. 이 흐름 자체(소스 push → 서버 pull → Docker로 테스트 실행)는 이 프로젝트의 확정된 테스트 방식이다.

테스트가 막혔을 때 아래 방식으로 우회하는 것을 금지한다:
- H2 인메모리 DB로 교체
- `@SpringBootTest` 제거 또는 Spring 컨텍스트 축소
- `spring.flyway.enabled=false`, `ddl-auto=create-drop` 등 설정 임시 변경

막혔을 때 올바른 접근:
- 근본 원인을 찾아 운영과 동일한 환경(MariaDB + `--network backend`)에서 동작하도록 해결한다.
- 해결 과정에서 생긴 임시 파일이나 설정 변경은 해결 즉시 원복한다. 사용자가 나중에 인지하고 지워야 하는 잔재를 남기지 않는다.

## 키움증권 API 문서 파악 원칙
목표 : docs/images/tobe 폴더 내 있는 두 이미지 파일을 구현하는 것이 목표

1. main 에이전트 + 8개의 멀티 에이전트를 구성한다. main 에이전트는 PM 의 역할, 8개의 에이전트는 개발자 역할이다.
2. 8개의 각 에이전트는 배정된 폴더에 있는 png 파일을 모두 읽고, 목표에 해당하는 대시보드 구성이 가능한 API를 찾고 구현에 필요한 정보를 정리하여 main 에이전트에게 전달
3. main 에이전트는 전달받은 정보를 바탕으로 가능 여부를 판단하고, 가능하다면 구현 계획을 수립한다.


