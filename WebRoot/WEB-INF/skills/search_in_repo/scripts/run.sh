#!/bin/bash
# Skill: search_in_repo
# Description: Full-text search within a specific repository
# Usage: run.sh <query> <vid>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

QUERY="${1:-}"
VID="${2:-}"

if [ -z "$QUERY" ] || [ -z "$VID" ]; then
  echo "Usage: run.sh <query> <vid>"
  echo "Example: run.sh reports 1"
  exit 1
fi

ENDPOINT="/api/search?q=$(echo "$QUERY" | sed 's/ /+/g')&vid=${VID}"

echo "Searching for '$QUERY' in repository VID: $VID"
curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
