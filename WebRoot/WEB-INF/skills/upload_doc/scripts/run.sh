#!/bin/bash
# Skill: upload_doc
# Description: Upload a local file to a repository
# Usage: run.sh <file> <vid> [pid]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

FILE="${1:-}"
VID="${2:-}"
PID="${3:-0}"

if [ -z "$FILE" ] || [ -z "$VID" ]; then
  echo "Usage: run.sh <file> <vid> [pid]"
  echo "Example: run.sh F:/report.pdf 1       # upload to root of repo 1"
  echo "         run.sh data.xlsx 2 5        # upload to folder 5 in repo 2"
  exit 1
fi

if [ ! -f "$FILE" ]; then
  echo "Error: File not found: $FILE"
  exit 1
fi

ENDPOINT="/api/docs/upload?vid=${VID}&pid=${PID}"

echo "Uploading file '$FILE' to repository VID: $VID, folder PID: $PID"
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -u "$CLI_USER:$CLI_PASS" \
  -F "file=@${FILE}" | jq .
