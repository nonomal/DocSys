#!/bin/bash
# Test: browser_use

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="$SCRIPT_DIR/../scripts/run.sh"

# Test 1: Missing task shows usage
if bash "$RUN_SCRIPT" 2>&1 | grep -q "Usage:"; then
  echo "PASS: Missing task shows usage"
else
  echo "FAIL: Should show usage without task"
  exit 1
fi

echo '{"result":"Flight found: Beijing to Tokyo - $500","stepsExecuted":5}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
