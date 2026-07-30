#!/bin/bash
# Skill: system_help
# Description: Show help and available commands
# Usage: run.sh [command]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

COMMAND="${1:-}"

if [ -n "$COMMAND" ]; then
  ENDPOINT="/api/help/${COMMAND}"
  echo "Getting help for command: $COMMAND"
else
  ENDPOINT="/api/help"
  echo "Getting general help"
fi

curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
