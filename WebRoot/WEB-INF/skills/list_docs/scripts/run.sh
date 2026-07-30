#!/bin/bash
# Skill: list_docs
# Description: List documents and folders in a repository or folder
# Usage: run.sh <vid> [pid]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"
PID="${2:-0}"

if [ -z "$VID" ]; then
  echo "Usage: run.sh <vid> [pid]"
  echo "Example: run.sh 1        # list root of repo 1"
  echo "         run.sh 1 5       # list folder 5 in repo 1"
  exit 1
fi

ENDPOINT="/api/repos/${VID}/docs?pid=${PID}"

echo "Listing documents in repository VID: $VID, folder PID: $PID"
curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
