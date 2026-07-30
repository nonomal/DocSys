#!/bin/bash
# Skill: whoami
# Description: Show current user info and session status
# Usage: run.sh

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

ENDPOINT="/api/auth/whoami"

echo "Getting current user info..."
curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
