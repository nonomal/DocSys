#!/bin/bash
# Skill: user_login
# Description: Login to DocSystem with username and password
# Usage: run.sh <username> <password>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"

USERNAME="${1:-}"
PASSWORD="${2:-}"

if [ -z "$USERNAME" ] || [ -z "$PASSWORD" ]; then
  echo "Usage: run.sh <username> <password>"
  echo "Example: run.sh admin admin2026"
  exit 1
fi

ENDPOINT="/api/auth/login"
PAYLOAD=$(jq -n --arg user "$USERNAME" --arg pass "$PASSWORD" \
  '{username: $user, password: $pass}')

echo "Logging in as: $USERNAME"
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD" | jq .
