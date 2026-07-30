#!/bin/bash
# Skill: move_doc
# Description: Move a document to another folder within the same repository
# Usage: run.sh <vid> <docId> <targetPid>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"
DOC_ID="${2:-}"
TARGET_PID="${3:-}"

if [ -z "$VID" ] || [ -z "$DOC_ID" ] || [ -z "$TARGET_PID" ]; then
  echo "Usage: run.sh <vid> <docId> <targetPid>"
  echo "Example: run.sh 1 123 5    # move doc 123 to folder 5"
  echo "         run.sh 1 123 0    # move doc 123 to root"
  exit 1
fi

ENDPOINT="/api/docs/${DOC_ID}/move?vid=${VID}"
PAYLOAD=$(jq -n --arg pid "$TARGET_PID" '{targetPid: ($pid | tonumber)}')

echo "Moving document $DOC_ID to folder $TARGET_PID in repository VID: $VID"
curl -s -X PUT "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" \
  -d "$PAYLOAD" | jq .
