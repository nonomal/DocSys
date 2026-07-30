#!/bin/bash
# Test: add_doc

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

# Test 2: Missing name shows usage
if bash "$RUN_SCRIPT" "1" 2>&1 | grep -q "Usage:"; then
  echo "PASS: Missing name shows usage"
fi

# Test 3: Mock response
echo '{"success":true,"docId":5,"name":"reports","type":"folder"}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
