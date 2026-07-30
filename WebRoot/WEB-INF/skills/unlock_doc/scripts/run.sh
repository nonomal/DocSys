#!/bin/bash
# Skill: unlock_doc
# Description: Unlock a document to allow editing
# Usage: run.sh <vid> <docId>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"
DOC_ID="${2:-}"

if [ -z "$VID" ] || [ -z "$DOC_ID" ]; then
  echo "Usage: run.sh <vid> <docId>"
  echo "Example: run.sh 1 123"
  exit 1
fi

ENDPOINT="/api/docs/${DOC_ID}/unlock?vid=${VID}"

echo "Unlocking document $DOC_ID in repository VID: $VID"
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
