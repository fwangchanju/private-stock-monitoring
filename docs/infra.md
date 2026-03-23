# 인프라 구성

## 서버 구성

Oracle Free Tier AMD 서버 2대.

- **1번 서버**: 앱 운영 서버. psmsapp, mariadb, nginx 컨테이너 운영.
- **2번 서버**: 빌드 서버. GitHub Actions로 대체됨 (반납 가능).

1번 서버에서 Spring Boot 빌드까지 하는 건 메모리 부족으로 무리. GitHub Actions runner(ubuntu-latest)에서 빌드 권장.

**서버 스펙 (1번 기준)**
- 디스크: 49GB (사용 중 약 9GB)
- 메모리: 956MB RAM / Swap 4GB

---

## CI/CD 파이프라인

워크플로우 파일: `.github/workflows/build-and-push.yml`

### 자동 빌드 + 배포 (`auto-build` 브랜치 push)

```
auto-build push
  → Docker 이미지 빌드 (psms + psms-nginx)
  → GHCR push
  → SSH로 1번 서버 접속 (deploy 유저)
  → deploy.sh 실행 (앱 + nginx 동시 재배포)
```

### 수동 실행 (GitHub Actions 탭 → Run workflow)

빌드 대상 선택:
- `app` → psms 이미지 빌드 + GHCR push + deploy-app.sh 실행
- `nginx` → psms-nginx 이미지 빌드 + GHCR push + deploy-nginx.sh 실행
- `all` → 둘 다 빌드 + GHCR push + deploy.sh 실행

### GitHub Secrets

| 이름 | 용도 |
|---|---|
| `SERVER_HOST` | 1번 서버 IP |
| `SERVER_USER` | SSH 접속 계정 (`deploy`) |
| `SERVER_SSH_KEY` | deploy 유저 SSH 개인키 |
| `CR_PAT` | GHCR pull용 Personal Access Token |

### 서버 배포 스크립트 (1번 서버)

- `deploy/deploy.sh` — 앱 + nginx 동시 배포
- `deploy/deploy-app.sh` — psms 앱만 배포
- `deploy/deploy-nginx.sh` — psms-nginx만 배포

---

## Docker 이미지

- `ghcr.io/fwangchanju/psms:latest` — Spring Boot 앱
- `ghcr.io/fwangchanju/psms-nginx:latest` — nginx + 프론트엔드 정적 파일 (멀티스테이지 빌드)
- 레지스트리: GHCR (GitHub Container Registry)

---

## 서버 디렉토리 구조

```
~/app/
  private-stock-monitoring/   ← 레포 clone 위치
    deploy/
      docker-compose.yml      ← psmsapp 컨테이너
      deploy.sh               ← 앱 + nginx 동시 배포
      deploy-app.sh           ← 앱만 배포
      deploy-nginx.sh         ← nginx만 배포

~/infra/
  db/
    docker-compose.yml        ← mariadb 컨테이너
  nginx/
    docker-compose.yml        ← nginx 컨테이너

~/env/
  private-stock-monitoring.env  ← 비밀값 환경변수 파일
```

---

## Docker 네트워크

- 네트워크명: `backend` (external)
- psmsapp, mariadb, nginx 모두 같은 네트워크 사용

---

## 컨테이너 구성

### psmsapp
- 이미지: `ghcr.io/fwangchanju/psms:latest`
- compose 위치: `~/app/private-stock-monitoring/deploy/docker-compose.yml`
- 포트: 외부 미노출 (nginx 통해서만 접근)

### mariadb
- 이미지: `mariadb:10.6`
- compose 위치: `~/infra/db/docker-compose.yml`
- 저메모리 최적화 옵션 적용 (innodb-buffer-pool-size=128M 등)
- 볼륨: `./maria_data`

### nginx
- 이미지: `ghcr.io/fwangchanju/psms-nginx:latest`
- compose 위치: `~/infra/nginx/docker-compose.yml`
- nginx.conf: 이미지에 포함 (`deploy/nginx/Dockerfile`에서 복사), nginx.conf 변경 시 이미지 재빌드 필요
- HTTPS: Duck DNS (`eolmae.duckdns.org`) + Let's Encrypt (Certbot)
- Basic Auth 적용 (`/etc/nginx/.htpasswd`)
- 프론트엔드 정적 파일 포함 (Node.js 빌드 → nginx 이미지에 내장)

---

## 환경변수 파일

위치: `~/env/private-stock-monitoring.env`

비밀값만 관리. 나머지는 `application-prod.properties`에 하드코딩.

```
DB_APP_PASSWD=
DB_ADM_PASSWD=
KIWOOM_APP_KEY=
KIWOOM_SECRET=
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

---

## DB 계정

- `psmsadm`: Flyway 마이그레이션 전용 (DDL 권한)
- `psmsapp`: 앱 런타임 전용 (SELECT, INSERT, UPDATE, DELETE)
