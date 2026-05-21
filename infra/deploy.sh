#!/bin/bash
set -e

SCRIPT_DIR="$(dirname "$0")"

bash "$SCRIPT_DIR/deploy-app.sh"
bash "$SCRIPT_DIR/deploy-front.sh"
bash "$SCRIPT_DIR/deploy-renderer.sh"

echo "=== All done ==="
