#!/bin/bash
# Skill: add_doc
# Description: Create a new folder in a repository
# Usage: run.sh <vid> <name>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"
NAME="${2:-}"

if [ -z "$VID" ] || [ -z "$NAME" ]; then
  echo "Usage: run.sh <vid> <name>"
  echo "Example: run.sh 1 reports"
  exit 1
fi

ENDPOINT="/api/repos/${VID}/docs"
PAYLOAD=$(jq -n --arg name "$NAME" '{name: $name, type: "folder"}')

echo "Creating folder '$NAME' in repository VID: $VID"
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" \
  -d "$PAYLOAD" | jq .
