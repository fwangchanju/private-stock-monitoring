-- Docker 사용 시 POSTGRES_USER, POSTGRES_DB 환경변수로 자동 생성됨
-- 수동 설치 시 postgres superuser로 실행
CREATE USER market_monitor WITH PASSWORD 'CHANGE_ME';
CREATE DATABASE market_monitor_db OWNER market_monitor;
