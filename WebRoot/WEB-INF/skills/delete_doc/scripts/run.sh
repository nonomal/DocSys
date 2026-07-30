#!/bin/bash
# Skill: delete_doc
# Description: Delete a document or folder from a repository
# Usage: run.sh <vid> <docId>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"
DOC_ID="${2:-}"

if [ -z "$VID" ] || [ -z "$DOC_ID" ]; then
  echo "Usage: run.sh <vid> <docId>"
  echo "Example: run.sh 1 789"
  exit 1
fi

ENDPOINT="/api/docs/${DOC_ID}?vid=${VID}"

echo "Deleting document DOC_ID: $DOC_ID from repository VID: $VID"
curl -s -X DELETE "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
