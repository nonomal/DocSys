#!/bin/bash
# Skill: create_repos
# Description: Create a new repository in DocSystem
# Usage: run.sh <name> <path>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

NAME="${1:-}"
PATH="${2:-}"

if [ -z "$NAME" ] || [ -z "$PATH" ]; then
  echo "Usage: run.sh <name> <path>"
  echo "Example: run.sh MyProject F:/data/myrepo"
  exit 1
fi

ENDPOINT="/api/repos"
PAYLOAD=$(jq -n --arg name "$NAME" --arg path "$PATH" \
  '{name: $name, path: $path}')

echo "Creating repository: $NAME at $PATH"
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" \
  -d "$PAYLOAD" | jq .
