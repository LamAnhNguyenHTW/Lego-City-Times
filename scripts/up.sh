#!/bin/sh
set -eu
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
"$SCRIPT_DIR/render-alertmanager.sh"
if [ "${1:-}" = "--build" ]; then
  docker compose up -d --build
else
  docker compose up -d
fi