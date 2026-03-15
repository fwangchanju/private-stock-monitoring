#!/bin/bash
set -e

COMPOSE_FILE="$(dirname "$0")/docker-compose.yml"
ENV_FILE="$HOME/env/.env"

echo "=== Pulling latest image ==="
docker pull ghcr.io/fwangchanju/psms:latest

echo "=== Restarting psmsapp ==="
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

echo "=== Done ==="
