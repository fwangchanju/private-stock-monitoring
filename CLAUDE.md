# PSMS (Private Stock Monitoring Service)

키움증권 REST API 기반 개인 주식 모니터링 대시보드. 정시 체크용 웹 대시보드 + 텔레그램 링크 리마인더로 구성.

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
- **2번 서버**: 빌드 서버. `deploy/build-and-push.sh`로 이미지 빌드 후 GHCR push.

배포 흐름: 2번에서 빌드 → GHCR push (psms + psms-nginx 2개) → 1번에서 `deploy/deploy.sh` 실행.

### Docker 네트워크

`backend` (external). mariadb와 psmsapp이 같은 네트워크.

### 환경변수 파일

1번 서버 `~/env/private-stock-monitoring.env`. **비밀값만** 관리:
- `DB_APP_PASSWD`, `DB_ADM_PASSWD`
- `KIWOOM_APP_KEY`, `KIWOOM_SECRET`
- `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`

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
├── external/          # 키움 API 연동 (미구현)
├── collector/         # 수집 오케스트레이션 (미구현)
└── notification/      # 텔레그램 발송 (미구현)
```

---

## 구현 현황

**완료**
- 도메인 레이어 전체 (Entity, Repository)
- 조회 API / QueryService / DTO (빈 데이터 처리, 랭킹 상세 엔드포인트 포함)
- Flyway 마이그레이션 V1 (스키마), V2 (샘플 데이터)
- 1번 서버에서 prod 프로파일로 앱 정상 기동
- 키움 API 연동 기반: KiwoomProperties, KiwoomTokenManager, KiwoomApiClient, KiwoomResponseParser
- 수집기 전체 (collector/): 7개 Collector + CollectionScheduler — **TR ID·필드명 TODO 잔존** (하단 참고)
- 텔레그램 리마인더 (notification/): TelegramClient, TelegramNotifier, NotificationScheduler
- 프론트엔드: React + Vite + TypeScript 전체 구현
  - 대시보드 페이지 (6개 섹션)
  - 상세 페이지 4종 (장중랭킹, 프로그램매매, 지수기여도, 종목상세)
- Nginx 이미지 빌드 통합 (`deploy/nginx/Dockerfile`: 프론트 빌드 → nginx)
- dashboard.base-url: `https://eolmae.duckdns.org`

**미완료 — 키움 API 수집기 TODO**

수집기 코드는 작성됐으나 아래 항목을 포털에서 확인 후 수정 필요:

| 수집기 | 수정 필요 항목 |
|---|---|
| `IntradayInvestorRankingCollector` | TR ID `ka10065`로 수정, API 경로, 요청 파라미터명, 응답 필드명 |
| `ProgramTradingCollector` | TR ID `ka90003`(랭킹)·`ka90008`/`ka90013`(추이)로 수정, 경로, 파라미터·필드명 |
| `ShortSellingCollector` | TR ID `ka10014`로 수정, 경로, 파라미터·필드명 |
| `StockMasterCollector` | TR ID `ka10099`로 수정, 경로, 파라미터·필드명 |
| `MarketOverviewCollector` | TR ID `ka20001`, 경로, 파라미터·필드명 전체 |
| `InvestorTradingSummaryCollector` | TR ID `ka10051`, 경로, 파라미터·필드명 전체 |
| `IndexContributionRankingCollector` | TR ID 미확인, 경로, 파라미터·필드명 전체 |

추가로 `KiwoomApiClient`의 헤더명 `tr_id` → `api-id` 수정 필요 (포털 예시 확인됨).

확인된 HTTP 경로: 순위정보 → `/api/dostk/rkinfo`. 나머지 카테고리 경로는 포털 직접 확인 필요.

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
