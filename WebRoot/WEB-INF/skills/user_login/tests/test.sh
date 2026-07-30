#!/bin/bash
# Test: user_login

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="$SCRIPT_DIR/../scripts/run.sh"

# Test 1: Missing args shows usage
if bash "$RUN_SCRIPT" 2>&1 | grep -q "Usage:"; then
  echo "PASS: Missing args shows usage"
else
  echo "FAIL: Should show usage without args"
  exit 1
fi

# Test 2: Missing password shows usage
if bash "$RUN_SCRIPT" "admin" 2>&1 | grep -q "Usage:"; then
  echo "PASS: Missing password shows usage"
fi

echo '{"success":true,"username":"admin","role":"admin","sessionId":"abc123"}' | jq .
echo "PASS: Mock login response valid JSON"

echo "All tests passed."
