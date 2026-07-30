#!/bin/bash
# Skill: list_repos
# Description: List all accessible repositories in DocSystem
# Usage: run.sh

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

ENDPOINT="/api/repos"

echo "Calling: $ENDPOINT"
curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
