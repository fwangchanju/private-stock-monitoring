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

- ❌ 메시지에 포함할 대시보드 링크 실제 동작 확인

## 운영 확인 작업

- ❓ 서버 재기동 후 컨테이너 자동 기동 여부 확인 (restart: always 설정됨, 실재기동 검증 필요)
- ✅ backend → mariadb 연결 확인 (Flyway 마이그레이션 정상 실행)
- ✅ nginx → backend 연결 확인 (대시보드 접속 확인)
- ✅ 외부에서 대시보드 URL 접속 확인
- ❌ 텔레그램 링크 클릭 시 정상 진입 확인 (수집기 완성 후)

---

## 참고

- 키움증권 REST API 키/토큰 1년마다 갱신 필요 → 갱신 방안 수립 필요

- ✅ GitHub Actions CI/CD 파이프라인 완성 (빌드 → GHCR push → SSH 자동 배포)

## TODO List

1. ❌ GHCR 이미지 패키지명 변경: `psms` → `psms-backend`, `psms-nginx` → `psms-frontend` (GHCR, docker-compose.yml, GitHub Actions 세 곳 함께 수정)
2. ✅ API 수집 및 적재 비즈니스 재설계 (7개 수집기 + KRX 크롤러 구현 완료)
3. ❌ `deploy/nginx/` 폴더명 → `deploy/front/` 로 변경 (docker-compose.yml, GitHub Actions, Dockerfile 경로 함께 수정)
4. ✅ HTTPS 적용 완료 (Duck DNS + Certbot, eolmae.duckdns.org)
5. ❌ 로그인 개선: 현재 Nginx Basic Auth — 번거로움 해소 방안 검토 필요
6. ❌ UI 개선: 현재 일반적인 React UI → 레트로 스타일 라이브러리 검토
   - **98.css**: Windows 98 스타일 (버튼, 창, 타이틀바 등 완벽 재현)
   - **XP.css**: Windows XP 스타일
   - **NES.css**: 8비트 픽셀 아트 스타일 (닌텐도 NES 느낌)
   - **Terminal.css** 계열: DOS/터미널 느낌 (흑백 or 녹색 글자)
   - 두 방향(98.css 계열 vs Terminal 계열) 각각 프로토타입 적용 후 비교해서 결정 예정
7. ❌ 텔레그램 이미지 자동 발송: `psms-screenshot` 컨테이너 구현 필요
   - Node.js + Express + Puppeteer (별도 Docker 컨테이너)
   - 수집 완료 후 Spring Boot → screenshot 서비스 HTTP 요청 → `sendPhoto` 발송
   - 프론트엔드: 수동 발송 트리거 버튼 추가 여부 검토


