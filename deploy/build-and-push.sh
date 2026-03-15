#!/bin/bash
set -e

IMAGE=ghcr.io/fwangchanju/private-stock-monitoring:latest

echo "=== Building Docker image ==="
docker build -t $IMAGE .

echo "=== Pushing to GHCR ==="
docker push $IMAGE

echo "=== Done ==="
