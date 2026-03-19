#!/bin/bash
set -e

APP_IMAGE=ghcr.io/fwangchanju/psms:latest
NGINX_IMAGE=ghcr.io/fwangchanju/psms-nginx:latest

echo "=== Logging in to GHCR ==="
echo "$CR_PAT" | docker login ghcr.io -u fwangchanju --password-stdin

echo "=== Building app image ==="
docker build -t $APP_IMAGE .

echo "=== Building nginx image (with frontend) ==="
docker build -f deploy/nginx/Dockerfile -t $NGINX_IMAGE .

echo "=== Pushing to GHCR ==="
docker push $APP_IMAGE
docker push $NGINX_IMAGE

echo "=== Done ==="
