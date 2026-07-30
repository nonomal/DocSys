#!/bin/bash
# Skill: rename_doc
# Description: Rename a document or folder within a repository
# Usage: run.sh <vid> <docId> <newName>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"
DOC_ID="${2:-}"
NEW_NAME="${3:-}"

if [ -z "$VID" ] || [ -z "$DOC_ID" ] || [ -z "$NEW_NAME" ]; then
  echo "Usage: run.sh <vid> <docId> <newName>"
  echo "Example: run.sh 1 123 new_report.pdf"
  exit 1
fi

ENDPOINT="/api/docs/${DOC_ID}?vid=${VID}"
PAYLOAD=$(jq -n --arg name "$NEW_NAME" '{name: $name}')

echo "Renaming document $DOC_ID to '$NEW_NAME' in repository VID: $VID"
curl -s -X PUT "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" \
  -d "$PAYLOAD" | jq .
