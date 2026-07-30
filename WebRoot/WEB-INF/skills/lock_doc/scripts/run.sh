#!/bin/bash
# Skill: lock_doc
# Description: Lock a document to prevent editing by others
# Usage: run.sh <vid> <docId> [type]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"
DOC_ID="${2:-}"
LOCK_TYPE="${3:-1}"

if [ -z "$VID" ] || [ -z "$DOC_ID" ]; then
  echo "Usage: run.sh <vid> <docId> [type]"
  echo "Example: run.sh 1 123         # exclusive lock (default)"
  echo "         run.sh 1 123 2        # shared lock"
  exit 1
fi

ENDPOINT="/api/docs/${DOC_ID}/lock?vid=${VID}"
PAYLOAD=$(jq -n --arg type "$LOCK_TYPE" '{type: ($type | tonumber)}')

echo "Locking document $DOC_ID in repository VID: $VID (type: $LOCK_TYPE)"
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" \
  -d "$PAYLOAD" | jq .
