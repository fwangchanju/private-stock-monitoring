# Private Stock Monitoring Service

개인용 주식 모니터링 대시보드
키움증권 REST API 활용

## 서버(스펙)

Oracle Free Tier AMD 서버 1 OCPU, 1GB RAM Ubuntu 22.04 Minimal
- 디스크: 49GB / 메모리: 956MB RAM + Swap 4GB

## Stack
### Bacnend
- Spring Boot 4.0 : 서버
- JPA : ORM
- Querydsl

### Database
- mariadb : Main DB
- Flyway : DB Migration

### Frontend
- React
- Vite
- TypeScript

### Infra
- Oracle : 클라우드
- Docker/Docker Compose
- GHCR : Image Repository
- Github Actions : CI/CD
- Nginx : Reverse Proxy
- Duck DNS : Domain
- Let's Encrypt : SSL

### Monitoring
# TODO

## 구현 계획
### 대시보드 구성 (7개 표)
| # | 표 | 설명 |
|---|---|---|
| 1 | 시장종합 | 코스피/코스닥 지수, 등락 종목수, 거래대금 요약 |
| 2 | 투자자별 매매종합 | 개인/외국인/기관 등 주체별 순매수 현황 |
| 3 | 장중 투자자별 매매 상위 | 특정 투자자 주체 기준 장중 상위 종목 랭킹 |
| 4 | 프로그램 순매수 상위 | 프로그램 매매 기준 순매수/순매도 상위 종목 |
| 5 | 종목별 프로그램매매추이 | 관심 종목의 시간별/일별 프로그램 매매 흐름 |
| 6 | 종목별 공매도추이 | 관심 종목의 일별 공매도 수량·비중 흐름 |
| 7 | 지수기여도 상위 | 지수 기여도 상위 종목 (전용 API 미확인, 보류) |
