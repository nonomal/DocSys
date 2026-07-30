#!/bin/bash
# Skill: repos_info
# Description: Get detailed information about a specific repository
# Usage: run.sh <vid>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VID="${1:-}"

if [ -z "$VID" ]; then
  echo "Usage: run.sh <vid>"
  echo "Example: run.sh 1"
  exit 1
fi

ENDPOINT="/api/repos/${VID}"

echo "Getting repository info for VID: $VID"
curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
