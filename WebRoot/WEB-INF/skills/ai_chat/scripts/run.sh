#!/bin/bash
# Skill: ai_chat
# Description: Chat with AI assistant for general questions and conversations
# Usage: run.sh <message> [model]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

MESSAGE="${1:-}"
MODEL="${2:-}"

if [ -z "$MESSAGE" ]; then
  echo "Usage: run.sh <message> [model]"
  echo "Example: run.sh 'what is DocSystem?'"
  echo "         run.sh 'explain this code' claude-3-5-sonnet"
  exit 1
fi

ENDPOINT="/api/chat"
if [ -n "$MODEL" ]; then
  PAYLOAD=$(jq -n --arg msg "$MESSAGE" --arg model "$MODEL" \
    '{message: $msg, model: $model}')
else
  PAYLOAD=$(jq -n --arg msg "$MESSAGE" '{message: $msg}')
fi

echo "Sending chat message..."
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" \
  -d "$PAYLOAD" | jq .
