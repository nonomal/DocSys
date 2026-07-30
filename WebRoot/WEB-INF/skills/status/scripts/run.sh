#!/bin/bash
# Skill: status
# Description: Show DocSys connection status and session info
# Usage: run.sh

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

ENDPOINT="/api/status"

echo "Checking DocSys status..."
curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
