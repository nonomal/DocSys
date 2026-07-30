#!/bin/bash
# Skill: web_search
# Description: Search the web using search engines (Baidu, Google, Bing)
# Usage: run.sh <query> [--engine baidu] [--limit 10]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

QUERY=""
ENGINE="baidu"
LIMIT=10

while [ $# -gt 0 ]; do
  case "$1" in
    --engine)
      ENGINE="${2:-baidu}"
      shift 2
      ;;
    --limit)
      LIMIT="${2:-10}"
      shift 2
      ;;
    -*)
      echo "Unknown option: $1"
      exit 1
      ;;
    *)
      QUERY="$1"
      shift
      ;;
  esac
done

if [ -z "$QUERY" ]; then
  echo "Usage: run.sh <query> [--engine baidu|google|bing] [--limit N]"
  echo "Example: run.sh 'Claude AI features'"
  echo "         run.sh '人工智能' --engine baidu --limit 5"
  exit 1
fi

ENDPOINT="/api/web-search?q=$(echo "$QUERY" | sed 's/ /+/g')&engine=${ENGINE}&limit=${LIMIT}"

echo "Web search for: $QUERY (engine: $ENGINE, limit: $LIMIT)"
curl -s -X GET "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" | jq .
