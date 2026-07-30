#!/bin/bash
# Skill: delete_repos
# Description: Delete a repository from DocSystem
# Usage: run.sh <vid>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"

if [ -z "$VID" ]; then
  echo "Usage: run.sh <vid>"
  echo "Example: run.sh 5"
  exit 1
fi

ENDPOINT="/api/repos/${VID}"

echo "Deleting repository VID: $VID"
curl -s -X DELETE "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
