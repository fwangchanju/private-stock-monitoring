#!/bin/bash
set -e

COMPOSE_FILE="$(dirname "$0")/docker-compose.yml"
ENV_FILE="$HOME/env/private-stock-monitoring.env"

echo "=== [app] Logging in to GHCR ==="
echo "$CR_PAT" | docker login ghcr.io -u fwangchanju --password-stdin

echo "=== [app] Pulling latest image ==="
docker pull ghcr.io/fwangchanju/psms:latest

echo "=== [app] Restarting psmsapp ==="
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

echo "=== [app] Done ==="
