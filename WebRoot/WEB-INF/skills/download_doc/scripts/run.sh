#!/bin/bash
# Skill: download_doc
# Description: Download a document to local filesystem
# Usage: run.sh <vid> <docId> [localPath]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"
DOC_ID="${2:-}"
LOCAL_PATH="${3:-.}"

if [ -z "$VID" ] || [ -z "$DOC_ID" ]; then
  echo "Usage: run.sh <vid> <docId> [localPath]"
  echo "Example: run.sh 1 123              # download to current dir"
  echo "         run.sh 1 123 /tmp/output   # download to /tmp/output"
  exit 1
fi

ENDPOINT="/api/docs/${DOC_ID}/download?vid=${VID}"

echo "Downloading document $DOC_ID from repository VID: $VID"
FILENAME=$(curl -s -I "${CLI_URL}${ENDPOINT}" \
  -u "$CLI_USER:$CLI_PASS" | grep -i "content-disposition" | \
  sed 's/.*filename=//;s/\r$//' || echo "document_${DOC_ID}")

if [ -d "$LOCAL_PATH" ]; then
  OUTPUT="${LOCAL_PATH}/${FILENAME}"
else
  OUTPUT="$LOCAL_PATH"
fi

curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -u "$CLI_USER:$CLI_PASS" -o "$OUTPUT"
echo "Downloaded to: $OUTPUT"
