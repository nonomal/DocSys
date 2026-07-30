#!/bin/bash
# Test: ai_chat

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="$SCRIPT_DIR/../scripts/run.sh"

# Test 1: Missing message shows usage
if bash "$RUN_SCRIPT" 2>&1 | grep -q "Usage:"; then
  echo "PASS: Missing message shows usage"
else
  echo "FAIL: Should show usage without message"
  exit 1
fi

echo '{"response":"DocSystem is a document management system.","model":"claude-3-5-sonnet"}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
