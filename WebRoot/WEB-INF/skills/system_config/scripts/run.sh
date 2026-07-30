#!/bin/bash
# Skill: system_config
# Description: Get or update DocSystem configuration (admin only)
# Usage: run.sh [--get <key>] [--set <key> <value>] [--list]

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

ACTION=""
KEY=""
VALUE=""

while [ $# -gt 0 ]; do
  case "$1" in
    --get)
      ACTION="get"
      KEY="${2:-}"
      shift 2
      ;;
    --set)
      ACTION="set"
      KEY="${2:-}"
      VALUE="${3:-}"
      shift 3
      ;;
    --list)
      ACTION="list"
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: run.sh [--get <key>] [--set <key> <value>] [--list]"
      exit 1
      ;;
  esac
done

if [ "$ACTION" = "list" ]; then
  ENDPOINT="/api/config?list=true"
  echo "Listing all configuration keys"
  curl -s -X GET "${CLI_URL}${ENDPOINT}" \
    -H "Content-Type: application/json" \
    -u "$CLI_USER:$CLI_PASS" | jq .

elif [ "$ACTION" = "get" ]; then
  if [ -z "$KEY" ]; then
    echo "Usage: run.sh --get <key>"
    exit 1
  fi
  ENDPOINT="/api/config/${KEY}"
  echo "Getting config key: $KEY"
  curl -s -X GET "${CLI_URL}${ENDPOINT}" \
    -H "Content-Type: application/json" \
    -u "$CLI_USER:$CLI_PASS" | jq .

elif [ "$ACTION" = "set" ]; then
  if [ -z "$KEY" ] || [ -z "$VALUE" ]; then
    echo "Usage: run.sh --set <key> <value>"
    exit 1
  fi
  PAYLOAD=$(jq -n --arg key "$KEY" --arg value "$VALUE" \
    '{key: $key, value: $value}')
  ENDPOINT="/api/config"
  echo "Setting config key '$KEY' = '$VALUE'"
  curl -s -X PUT "${CLI_URL}${ENDPOINT}" \
    -H "Content-Type: application/json" \
    -u "$CLI_USER:$CLI_PASS" \
    -d "$PAYLOAD" | jq .

else
  ENDPOINT="/api/config"
  echo "Getting all configuration"
  curl -s -X GET "${CLI_URL}${ENDPOINT}" \
    -H "Content-Type: application/json" \
    -u "$CLI_USER:$CLI_PASS" | jq .
fi
