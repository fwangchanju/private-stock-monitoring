## 문서 역할

이 문서는 사용자가 리눅스 서버에서 직접 수행해야 하는 작업만 정리한다.
설계 방향이나 미확정 설계 사안은 `plan.md`에서 관리한다.

> 상태 표기: ✅ 완료 / ❌ 미완료 / 🔄 진행중·부분완료 / ❓ 확인필요

---

## 서버 / 인프라 작업

- ✅ Docker / Docker Compose 실행 환경 준비
- ❌ nginx 컨테이너 구성
- ✅ backend 컨테이너 구성 (psmsapp, GHCR 이미지 pull·기동 확인)
- ✅ mariadb 컨테이너 구성 (mariadb:10.6, healthy 상태)
- ✅ 컨테이너 간 네트워크 구성 (ptas-network)
- ✅ 데이터 저장 볼륨 경로 설정 (maria_data)
- ❓ 로그 파일 및 디스크 사용량 관리 방식 반영

## nginx / 접근 경로 작업

- ❌ 외부 진입용 대시보드 URL 결정
- ❌ nginx reverse proxy 설정
- ❌ `/api` 경로를 backend로 프록시하도록 설정
- ❌ 정적 프론트와 API 경로가 충돌하지 않도록 확인
- ❌ 필요 시 HTTPS / 인증서 적용 여부 검토 및 반영

## MariaDB 작업

- ❌ psmsdb 스키마 생성
- ❌ psms 전용 계정 생성 및 권한 부여 (adm, app 유저)
- 🔄 접속 정보 정리 (env 파일 구조 확정, 서버 .env 값 입력 필요)
- ❌ 백업/복구 기본 방식 메모

## 환경변수 / 시크릿 작업

- ✅ 키움 API 인증정보 주입 방식 정리 (.env → KIWOOM_APP_KEY, KIWOOM_SECRET)
- ✅ Telegram Bot Token / Chat ID 설정
- ✅ Spring 환경변수 파일 구성 (application.properties, application-prod.properties)
- ✅ base URL 설정 (DASHBOARD_BASE_URL)
- ✅ 운영/개발 환경값 분리 (SPRING_PROFILES_ACTIVE=prod)

## 텔레그램 작업

- ❌ 메시지에 포함할 대시보드 링크 실제 동작 확인

## 운영 확인 작업

- ❓ 서버 재기동 후 컨테이너 자동 기동 여부 확인 (restart: always 설정됨, 실재기동 검증 필요)
- ❌ nginx → backend 연결 확인
- ❌ backend → mariadb 연결 확인 (psmsdb 스키마 생성 후)
- ❌ 외부에서 대시보드 URL 접속 확인
- ❌ 텔레그램 링크 클릭 시 정상 진입 확인

---

키움증권 REST API 키/토큰 1년마다 갱신 필요
 - 잊지 않고 갱신 방안 수립 필요
