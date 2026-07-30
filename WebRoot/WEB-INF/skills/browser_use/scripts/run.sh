#!/bin/bash
# Skill: browser_use
# Description: AI-powered web browsing via browser-use.com API
# Usage: run.sh <task> [--url <url>] [--max-steps N]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

TASK=""
URL=""
MAX_STEPS=10

while [ $# -gt 0 ]; do
  case "$1" in
    --url)
      URL="${2:-}"
      shift 2
      ;;
    --max-steps)
      MAX_STEPS="${2:-10}"
      shift 2
      ;;
    -*)
      echo "Unknown option: $1"
      exit 1
      ;;
    *)
      TASK="$1"
      shift
      ;;
  esac
done

if [ -z "$TASK" ]; then
  echo "Usage: run.sh <task> [--url <url>] [--max-steps N]"
  echo "Example: run.sh 'find cheapest flight Beijing to Tokyo'"
  echo "         run.sh 'compare prices' --url https://amazon.com --max-steps 15"
  exit 1
fi

ENDPOINT="/api/browser-use"
if [ -n "$URL" ]; then
  PAYLOAD=$(jq -n --arg task "$TASK" --arg url "$URL" \
    --argjson maxSteps "$MAX_STEPS" \
    '{task: $task, url: $url, maxSteps: $maxSteps}')
else
  PAYLOAD=$(jq -n --arg task "$TASK" \
    --argjson maxSteps "$MAX_STEPS" \
    '{task: $task, maxSteps: $maxSteps}')
fi

echo "Running AI browser task: $TASK"
curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" \
  -d "$PAYLOAD" | jq .
