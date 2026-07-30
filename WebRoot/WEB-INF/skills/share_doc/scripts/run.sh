#!/bin/bash
# Skill: share_doc
# Description: List or create document sharing links
# Usage: run.sh list <vid> <docId>
#        run.sh create <vid> <docId> [type] [password]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

ACTION="${1:-}"
VID="${2:-}"
DOC_ID="${3:-}"
SHARE_TYPE="${4:-0}"
PASSWORD="${5:-}"

if [ "$ACTION" = "list" ]; then
  if [ -z "$VID" ] || [ -z "$DOC_ID" ]; then
    echo "Usage: run.sh list <vid> <docId>"
    exit 1
  fi
  ENDPOINT="/api/docs/${DOC_ID}/shares?vid=${VID}"
  echo "Listing share links for document $DOC_ID in repository VID: $VID"
  curl -s -X GET "${CLI_URL}${ENDPOINT}" \
    -H "Content-Type: application/json" \
    -u "$CLI_USER:$CLI_PASS" | jq .

elif [ "$ACTION" = "create" ]; then
  if [ -z "$VID" ] || [ -z "$DOC_ID" ]; then
    echo "Usage: run.sh create <vid> <docId> [type] [password]"
    exit 1
  fi
  ENDPOINT="/api/docs/${DOC_ID}/shares?vid=${VID}"
  if [ -n "$PASSWORD" ]; then
    PAYLOAD=$(jq -n --arg type "$SHARE_TYPE" --arg password "$PASSWORD" \
      '{type: ($type | tonumber), password: $password}')
  else
    PAYLOAD=$(jq -n --arg type "$SHARE_TYPE" '{type: ($type | tonumber)}')
  fi
  echo "Creating share link for document $DOC_ID in repository VID: $VID"
  curl -s -X POST "${CLI_URL}${ENDPOINT}" \
    -H "Content-Type: application/json" \
    -u "$CLI_USER:$CLI_PASS" \
    -d "$PAYLOAD" | jq .

else
  echo "Usage: run.sh list <vid> <docId>"
  echo "       run.sh create <vid> <docId> [type] [password]"
  echo "  type: 0=public, 1=password, 2=private"
  exit 1
fi
