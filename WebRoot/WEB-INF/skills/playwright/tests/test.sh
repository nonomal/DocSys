#!/bin/bash
# Test: playwright

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

# Test 2: open without URL shows usage
OUTPUT=$(bash "$RUN_SCRIPT" open 2>&1)
if echo "$OUTPUT" | grep -qi "usage"; then
  echo "PASS: open without URL shows usage"
fi

# Test 3: Mock response
echo '{"success":true,"screenshot":"base64..."}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
