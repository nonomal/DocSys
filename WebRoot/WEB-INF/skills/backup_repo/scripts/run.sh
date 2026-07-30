#!/bin/bash
# Skill: backup_repo
# Description: Start a repository backup and check its status
# Usage: run.sh <vid> [path]
#        run.sh --status <task-id>

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

ACTION="${1:-}"
VID="${2:-}"
TASK_PATH="${3:-}"

if [ "$ACTION" = "--status" ]; then
  TASK_ID="$VID"
  if [ -z "$TASK_ID" ]; then
    echo "Usage: run.sh --status <task-id>"
    exit 1
  fi
  ENDPOINT="/api/repos/backup/status/${TASK_ID}"
  echo "Checking backup status for task: $TASK_ID"
  curl -s -X GET "${CLI_URL}${ENDPOINT}" \
    -H "Content-Type: application/json" \
    -u "$CLI_USER:$CLI_PASS" | jq .
else
  VID="$ACTION"
  if [ -z "$VID" ]; then
    echo "Usage: run.sh <vid> [path]"
    echo "       run.sh --status <task-id>"
    exit 1
  fi
  PAYLOAD="{}"
  if [ -n "$TASK_PATH" ]; then
    PAYLOAD=$(jq -n --arg path "$TASK_PATH" '{path: $path}')
  fi
  ENDPOINT="/api/repos/${VID}/backup"
  echo "Starting backup for repository VID: $VID"
  curl -s -X POST "${CLI_URL}${ENDPOINT}" \
    -H "Content-Type: application/json" \
    -u "$CLI_USER:$CLI_PASS" \
    -d "$PAYLOAD" | jq .
fi
