#!/bin/bash
set -e

COMPOSE_FILE="$(dirname "$0")/docker-compose.yml"
ENV_FILE="$HOME/env/private-stock-monitoring.env"

echo "=== Logging in to GHCR ==="
echo "$CR_PAT" | docker login ghcr.io -u fwangchanju --password-stdin

echo "=== Pulling latest images ==="
docker pull ghcr.io/fwangchanju/psms:latest
docker pull ghcr.io/fwangchanju/psms-nginx:latest

echo "=== Restarting psmsapp ==="
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

echo "=== Restarting nginx ==="
docker restart nginx

echo "=== Done ==="
