오라클 프리티어 AMD 서버 2대 가동 중.



1번 서버를 앱 운영 서버로 계획 중이고, 1번 서버에서 스프링 도커 빌드까지 하는것은 무리라고 판단.

2번 서버에서 도커 이미지 빌드 후 깃허브에 push.

1번 서버에서 이미지 pull 후 기존 인스턴스 내리고 새로운 이미지로 인스턴스 생성하여 기동하는 방식

깃허브 액션을 통한 자동화 구현은 아직, GHCR 을 통한 이미지 push \& pull 에 대한 테스트는 완료.



서버 스펙

deploy@amd-ubuntu22-04:\~/env$ df -h

Filesystem      Size  Used Avail Use% Mounted on

tmpfs            96M  1.3M   95M   2% /run

efivarfs        256K   17K  235K   7% /sys/firmware/efi/efivars

/dev/sda1        49G  9.3G   40G  20% /

tmpfs           479M     0  479M   0% /dev/shm

tmpfs           5.0M     0  5.0M   0% /run/lock

/dev/sda15      105M  6.1M   99M   6% /boot/efi

tmpfs            96M  4.0K   96M   1% /run/user/1001

deploy@amd-ubuntu22-04:\~/env$

deploy@amd-ubuntu22-04:\~/env$ free -h

&#x20;              total        used        free      shared  buff/cache   available

Mem:           956Mi       397Mi       129Mi       0.0Ki       429Mi       399Mi

Swap:          4.0Gi       352Mi       3.7Gi

deploy@amd-ubuntu22-04:\~/env$



DB 관련 컴포즈 설정

redis 는 이전 작업에서 필요하여 추가함. 현재는 사용하지 않는 상태.

deploy@amd-ubuntu22-04:\~/infra/db$ cat docker-compose.yml

version: '3.8'



services:

&#x20; mariadb:

&#x20;   image: mariadb:10.6

&#x20;   container\_name: mariadb

&#x20;   restart: always

&#x20;   environment:

&#x20;     - MARIADB\_ROOT\_PASSWORD=${MARIADB\_ROOT\_PASSWORD}

&#x20;     - MARIADB\_DATABASE=ptasdb

&#x20;     - MARIADB\_USER=ptasapp

&#x20;     - MARIADB\_PASSWORD=${MARIADB\_PASSWORD}

&#x20;     - TZ=Asia/Seoul

&#x20;   command: \[

&#x20;     "mysqld",

&#x20;     "--innodb-buffer-pool-size=128M",

&#x20;     "--innodb-log-buffer-size=16M",

&#x20;     "--max-connections=20",



&#x20;     "--tmp-table-size=32M",

&#x20;     "--max-heap-table-size=32M",



&#x20;     "--sort-buffer-size=1M",

&#x20;     "--join-buffer-size=1M",

&#x20;     "--read-buffer-size=1M",

&#x20;     "--read-rnd-buffer-size=1M",



&#x20;     "--table-open-cache=200",

&#x20;     "--thread-cache-size=16",



&#x20;     "--skip-name-resolve"

&#x20;   ]

&#x20;   ports:

&#x20;     - "3306:3306"

&#x20;   volumes:

&#x20;     - ./maria\_data:/var/lib/mysql

&#x20;   networks:

&#x20;     - ptas-network

&#x20;   healthcheck:

&#x20;     test: \["CMD", "mariadb-admin", "ping", "-h", "127.0.0.1", "-uroot", "-p${MARIADB\_ROOT\_PASSWORD}"]

&#x20;     interval: 10s

&#x20;     timeout: 5s

&#x20;     retries: 10



&#x20; redis:

&#x20;   image: redis:7.2

&#x20;   container\_name: redis

&#x20;   restart: always

&#x20;   environment:

&#x20;     - TZ=Asia/Seoul

&#x20;   ports:

&#x20;     - "6379:6379"

&#x20;   volumes:

&#x20;     - ./redis\_data:/data

&#x20;   command: \[

&#x20;     "redis-server",

&#x20;     "--save", "",

&#x20;     "--appendonly", "no",

&#x20;     "--maxmemory", "128mb",

&#x20;     "--maxmemory-policy", "allkeys-lru"

&#x20;   ]

&#x20;   networks:

&#x20;     - ptas-network

&#x20;   healthcheck:

&#x20;     test: \["CMD", "redis-cli", "ping"]

&#x20;     interval: 10s

&#x20;     timeout: 3s

&#x20;     retries: 10



networks:

&#x20; ptas-network:

&#x20;   external: true



도커 프로세스 현황

deploy@amd-ubuntu22-04:\~/infra/db$ docker ps

CONTAINER ID   IMAGE                                                 COMMAND                  CREATED      STATUS                PORTS                                         NAMES

dc622129d7a4   ghcr.io/fwangchanju/private-stock-monitoring:latest   "java -jar app.jar"      2 days ago   Up 6 seconds          0.0.0.0:8080->8080/tcp, \[::]:8080->8080/tcp   psmsapp

d7aef4638bac   mariadb:10.6                                          "docker-entrypoint.s…"   4 days ago   Up 4 days (healthy)   0.0.0.0:3306->3306/tcp, \[::]:3306->3306/tcp   mariadb

deploy@amd-ubuntu22-04:\~/infra/db$



## Docker 이미지 이름 규칙

GHCR 이미지명은 `psms` 로 통일한다.

- 확정 이미지: `ghcr.io/fwangchanju/psms:latest`
- 이전 이미지명 `ghcr.io/fwangchanju/private-stock-monitoring:latest` 는 더 이상 사용하지 않음
- 1번 서버에서 기존 컨테이너 교체 시 이전 이미지는 `docker rmi` 로 정리

