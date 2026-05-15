## 문서 역할

이 문서는 리눅스 서버에서 직접 수행해야 하는 작업만 정리한다.
설계 방향이나 미확정 설계 사안은 `plan.md`에서 관리한다.

> 상태 표기: ✅ 완료 / ❌ 미완료 / 🔄 진행중·부분완료 / ❓ 확인필요

---

## 서버 / 인프라 작업

- ✅ Docker / Docker Compose 실행 환경 준비
- ✅ backend 컨테이너 구성 (psmsapp, GHCR 이미지 pull·기동 확인)
- ✅ mariadb 컨테이너 구성 (mariadb:10.6, healthy 상태)
- ✅ 컨테이너 간 네트워크 구성 (backend network)
- ✅ 데이터 저장 볼륨 경로 설정 (maria_data)
- ✅ GitHub Actions 워크플로우 구성 (auto-build 자동 / main 수동, app·nginx 선택 빌드)
- ✅ nginx 컨테이너를 `psms-nginx:latest` 이미지로 교체
- ❓ 로그 파일 및 디스크 사용량 관리 방식 반영

## nginx / 접근 경로 작업

- ✅ 외부 진입용 도메인 결정 (eolmae.duckdns.org)
- ✅ HTTPS 인증서 발급 (Let's Encrypt / Certbot)
- ✅ Basic Auth 파일 생성 (/etc/nginx/.htpasswd)
- ✅ nginx.conf 작성 완료 (reverse proxy + 정적 파일 서빙 설정)
- ✅ 프론트엔드 구현 완료 (psms-nginx 이미지에 내장)
- ✅ psms-nginx 이미지로 교체 후 외부 접속·API 라우팅 동작 확인

## MariaDB 작업

- ✅ psmsdb 스키마 생성
- ✅ psmsadm / psmsapp 계정 생성 및 권한 부여
- ✅ 접속 정보 및 env 파일 구성 완료
- ❌ 백업/복구 기본 방식 메모

## 환경변수 / 시크릿 작업

- ✅ 키움 API 인증정보 주입 방식 정리 (KIWOOM_APP_KEY, KIWOOM_SECRET)
- ✅ Telegram Bot Token / Chat ID 설정
- ✅ Spring 환경변수 파일 구성 (application.properties, application-prod.properties)
- ✅ 운영/개발 환경값 분리 (SPRING_PROFILES_ACTIVE=prod)
- ✅ 비밀값만 .env 관리, 나머지 properties 하드코딩으로 정리

## 텔레그램 작업

- ✅ 텔레그램 봇 sendPhoto 구현 완료
- ❌ 수집 완료 후 자동 발송 스케줄 연동 (현재 수동 버튼만)

## 운영 확인 작업

- ❓ 서버 재기동 후 컨테이너 자동 기동 여부 확인 (restart: always 설정됨, 실재기동 검증 필요)
- ✅ backend → mariadb 연결 확인 (Flyway 마이그레이션 정상 실행)
- ✅ nginx → backend 연결 확인 (대시보드 접속 확인)
- ✅ 외부에서 대시보드 URL 접속 확인
- ❌ 텔레그램 링크 클릭 시 정상 진입 확인 (수집기 완성 후)

---

## KRX 크롤링 세션 관리

- KRX 데이터 포털(`data.krx.co.kr`) 로그인 필수 정책 → 세션 쿠키 없으면 LOGOUT 반환
- 현재: 쿠키 파일(`~/env/krx-login-cookie`) 수동 관리 방식
- **변경 예정**: 매 수집 전 자동 로그인으로 대체 (쿠키 파일 방식 제거)

## 참고

- 키움증권 REST API 키/토큰 1년마다 갱신 필요 → 갱신 방안 수립 필요

- ✅ GitHub Actions CI/CD 파이프라인 완성 (빌드 → GHCR push → SSH 자동 배포)

## 프로젝트 이름 변경 작업 (mini PC 이전 시 함께 처리)

코드 레벨은 완료. 아래는 실제 서버 인프라와 묶인 항목들.

- [ ] GitHub 레포 이름 변경: `private-stock-monitoring` → `market-monitor-backend`
- [ ] GitHub Actions 워크플로우: 이미지 이름 `psms*` → `market-monitor*` 수정
- [ ] GHCR 이미지: `psms` / `psms-front` / `psms-renderer` → `market-monitor` / `market-monitor-front` / `market-monitor-renderer`
- [ ] 서버 컨테이너 재배포: `psmsapp` → `market-monitor-app`, `psms-renderer` → `market-monitor-renderer`
- [ ] `deploy/docker-compose.yml`, `infra/docker-compose.yml` 컨테이너 이름 수정
- [ ] `application-prod.properties`: `psms-renderer`, `psms-mariadb` 호스트명 수정
- [ ] 환경변수 파일 이름 변경: `~/env/private-stock-monitoring.env` → `~/env/market-monitor.env`
- [ ] DB 계정 및 스키마: `psmsdb` / `psmsapp` / `psmsadm` → PostgreSQL 이전 시 새 이름으로 생성

---

## TODO List

1. ❌ GHCR 이미지 패키지명 변경: `psms` → `market-monitor` (위 프로젝트 이름 변경 작업으로 대체)
2. ✅ API 수집 및 적재 비즈니스 재설계 (7개 수집기 + KRX 크롤러 구현 완료)
3. ✅ `deploy/nginx/` → `containers/front/` 구조 개편 완료
4. ✅ HTTPS 적용 완료 (Duck DNS + Certbot, eolmae.duckdns.org)
5. ❌ 로그인 개선: 현재 Nginx Basic Auth — mini PC 이전 후 검토
6. ❌ UI 개선: 레트로 스타일 프로토타입 3종 구현됨 (`/a` DOS, `/b` 98, `/c` NES) — 최종 스타일 미결정
7. 🔄 텔레그램 이미지 발송: `psms-renderer` 컨테이너 구현 완료, 수동 버튼 구현됨 — end-to-end 테스트 및 자동 발송 연동 필요


---

## 방향 전환 정리

### 배경

- 오라클 프리티어 AMD(1GB RAM) → **mini PC(32GB Memory, 1TB Disk)** 로 이전 예정
- 현재 레포지토리는 신규생성 x, 이름만 변경. 커밋 이력.

### 인프라 변경 (확정)

| 항목 | 현재 | 변경 후 |
|---|---|---|
| 서버 | Oracle Free Tier AMD 2대 | mini PC 단일 호스트 |
| DB | MariaDB 10.6 | PostgreSQL |
| GitHub Actions 배포 대상 | Oracle 서버 SSH | mini PC SSH |

### 프로젝트 구조 변경 (확정)

- **레포지토리 이름 변경**: `private-stock-monitoring` → `market-monitor-backend` / `market-monitor-frontend`
- **Spring context-path**: `/market-monitor` (API URL prefix)
- **프론트엔드 분리**: 별도 레포지토리(`market-monitor-frontend`)로 분리
- **nginx 설정 재구성**: 멀티 앱 라우팅 대응 (`/market-monitor` → 이 서비스)

### 기능 방향 추가 (중장기)

- 수집 데이터 이력 기반 이상 감지: 현재 사이클 수집값 vs 과거 동일 시간대 추이 비교
  - 예) 외국인 순매수가 평소 +100억대인데 이번에 -1500억 → 알림 트리거
- 로컬 LLM 연동: 이상 감지 이벤트 발생 시 원인 분석 (외부 API 미사용)
- 뉴스 수집 + 임베딩 (필요 시): PostgreSQL 전환 이유 중 하나

### 당장 영향 없는 것

- 비즈니스 로직(수집기, API, 대시보드 구조)은 그대로 유지
- 현재 진행 중인 KRX 자동 로그인, 스크린샷 발송 기능은 계속 개발

### 이전 시 주요 작업 목록 (mini PC 준비되면)

- [ ] PostgreSQL 마이그레이션 (Flyway 스크립트 재작성)
- [ ] 레포지토리 이름 변경 및 프론트 분리
- [ ] GitHub Actions 배포 대상 서버 변경
- [ ] nginx 재구성
- [ ] psms-renderer 서버 2 이전 → mini PC 이전으로 대체

---

## 작업 순서 (현재 기준)

### mini PC 도착 전 — 기능 완성

1. **KRX 자동 로그인 구현**
   - `KrxLoginTest` 기반 OkHttp 로그인 흐름을 `KrxCrawler`에 통합
   - 매 KRX 수집 전 자동 로그인 → 쿠키 획득 → 요청 흐름으로 대체
   - 쿠키 파일 수동 관리 방식 및 `extendSession` 제거

2. **수집 스케줄링 정리 및 통합 테스트**
   - `CollectionScheduler`에서 extendSession 호출 제거
   - 로컬 Docker 환경에서 수집기 통합 테스트 (order1~10)

3. **스크린샷 발송 end-to-end 테스트**
   - 로컬 Docker로 renderer 컨테이너 기동 후 전체 흐름 확인

### mini PC 도착 후 — 인프라 마이그레이션

4. 위 "이전 시 주요 작업 목록" 순서대로 진행
