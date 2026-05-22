#!/bin/sh
set -eu
ENV_FILE=".env"
TEMPLATE="monitoring/alertmanager/alertmanager.yml.tmpl"
OUT="monitoring/alertmanager/alertmanager.generated.yml"
if [ ! -f "$ENV_FILE" ]; then
  echo ".env not found" >&2
  exit 1
fi
SLACK_API_URL="$(grep -E '^SLACK_API_URL=' "$ENV_FILE" | cut -d'=' -f2-)"
if [ -z "$SLACK_API_URL" ]; then
  echo "SLACK_API_URL not set in .env" >&2
  exit 1
fi
sed "s|\${SLACK_API_URL}|$SLACK_API_URL|g" "$TEMPLATE" > "$OUT"
echo "Rendered $OUT"#!/bin/sh
set -eu
ENV_FILE=".env"
TEMPLATE="monitoring/alertmanager/alertmanager.yml.tmpl"
OUT="monitoring/alertmanager/alertmanager.generated.yml"
if [ ! -f "$ENV_FILE" ]; then
  echo ".env not found" >&2
  exit 1
fi
SLACK_API_URL="$(grep -E '^SLACK_API_URL=' "$ENV_FILE" | cut -d'=' -f2-)"
if [ -z "$SLACK_API_URL" ]; then
  echo "SLACK_API_URL not set in .env" >&2
  exit 1
fi
sed "s|\${SLACK_API_URL}|$SLACK_API_URL|g" "$TEMPLATE" > "$OUT"
echo "Rendered $OUT"