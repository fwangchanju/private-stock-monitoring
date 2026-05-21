#!/bin/bash
set -e

NGINX_COMPOSE_FILE="$HOME/infra/nginx/docker-compose.yml"

echo "=== [front] Logging in to GHCR ==="
echo "$CR_PAT" | docker login ghcr.io -u fwangchanju --password-stdin

echo "=== [front] Pulling latest image ==="
docker pull ghcr.io/fwangchanju/psms-nginx:latest

echo "=== [front] Restarting nginx ==="
docker compose -f "$NGINX_COMPOSE_FILE" down
docker compose -f "$NGINX_COMPOSE_FILE" up -d

echo "=== [front] Done ==="
