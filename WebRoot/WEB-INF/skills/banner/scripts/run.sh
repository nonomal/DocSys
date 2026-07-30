#!/bin/bash
# Skill: banner
# Description: Display the DocSys welcome banner
# Usage: run.sh [name]

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

NAME="${1:-}"

if [ -n "$NAME" ]; then
  ENDPOINT="/api/banner/${NAME}"
  echo "Displaying banner: $NAME"
else
  ENDPOINT="/api/banner"
  echo "Displaying default welcome banner"
fi

curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
