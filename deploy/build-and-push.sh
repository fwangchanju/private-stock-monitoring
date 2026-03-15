#!/bin/bash
set -e

IMAGE=ghcr.io/fwangchanju/psms:latest

echo "=== Building Docker image ==="
docker build -t $IMAGE .

echo "=== Pushing to GHCR ==="
docker push $IMAGE

echo "=== Done ==="
