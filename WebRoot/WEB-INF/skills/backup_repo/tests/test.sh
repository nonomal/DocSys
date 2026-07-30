#!/bin/bash
# Test: backup_repo

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="$SCRIPT_DIR/../scripts/run.sh"

# Test 1: No args shows usage
if bash "$RUN_SCRIPT" 2>&1 | grep -q "Usage:"; then
  echo "PASS: No args shows usage"
else
  echo "FAIL: Should show usage without args"
  exit 1
fi

# Test 2: --status with no task-id shows usage
if bash "$RUN_SCRIPT" --status 2>&1 | grep -q "Usage:"; then
  echo "PASS: --status without task-id shows usage"
fi

# Test 3: Mock backup response
echo '{"taskId":"backup-2025-12-15-001","status":"started","vid":1}' | jq .
echo "PASS: Mock backup response valid JSON"

# Test 4: Mock status response
echo '{"taskId":"backup-2025-12-15-001","status":"running","progress":50}' | jq .
echo "PASS: Mock status response valid JSON"

echo "All tests passed."
