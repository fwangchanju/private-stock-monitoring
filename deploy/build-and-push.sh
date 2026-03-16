#!/bin/bash
set -e

IMAGE=ghcr.io/fwangchanju/psms:latest

echo "=== Logging in to GHCR ==="
echo "$CR_PAT" | docker login ghcr.io -u fwangchanju --password-stdin

echo "=== Building Docker image ==="
docker build -t $IMAGE .

echo "=== Pushing to GHCR ==="
docker push $IMAGE

echo "=== Done ==="
