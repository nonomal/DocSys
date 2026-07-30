#!/bin/bash
# Skill: search_doc
# Description: Full-text search across all repositories
# Usage: run.sh <query> [vid]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

QUERY="${1:-}"
VID="${2:-}"

if [ -z "$QUERY" ]; then
  echo "Usage: run.sh <query> [vid]"
  echo "Example: run.sh project           # search all repos"
  echo "         run.sh report 1          # search repo 1 only"
  exit 1
fi

if [ -n "$VID" ]; then
  ENDPOINT="/api/search?q=$(echo "$QUERY" | sed 's/ /+/g')&vid=${VID}"
else
  ENDPOINT="/api/search?q=$(echo "$QUERY" | sed 's/ /+/g')"
fi

echo "Searching for: $QUERY${VID:+ in repository VID: $VID}"
curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
