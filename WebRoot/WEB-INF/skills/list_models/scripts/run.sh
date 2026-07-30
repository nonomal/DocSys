#!/bin/bash
# Skill: list_models
# Description: List all available AI models for chat and RAG
# Usage: run.sh [--verbose]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

VERBOSE="false"
if [ "${1:-}" = "--verbose" ]; then
  VERBOSE="true"
fi

ENDPOINT="/api/models?verbose=${VERBOSE}"

echo "Listing available AI models (verbose: $VERBOSE)"
curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
