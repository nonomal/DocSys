#!/bin/bash
# Skill: user_logout
# Description: Logout from DocSystem and clear current session
# Usage: run.sh

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

ENDPOINT="/api/auth/logout"

echo "Logging out..."
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
