#!/bin/bash
# Skill: rag_chat
# Description: Chat with AI using relevant documents as context (RAG)
# Usage: run.sh <query> [model]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

QUERY="${1:-}"
MODEL="${2:-}"

if [ -z "$QUERY" ]; then
  echo "Usage: run.sh <query> [model]"
  echo "Example: run.sh 'what does the Q4 report say?'"
  exit 1
fi

ENDPOINT="/api/rag"
if [ -n "$MODEL" ]; then
  PAYLOAD=$(jq -n --arg q "$QUERY" --arg model "$MODEL" \
    '{query: $q, model: $model}')
else
  PAYLOAD=$(jq -n --arg q "$QUERY" '{query: $q}')
fi

echo "Sending RAG query..."
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" \
  -d "$PAYLOAD" | jq .
