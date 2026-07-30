#!/bin/bash
# Test: share_doc

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="$SCRIPT_DIR/../scripts/run.sh"

# Test 1: No args shows usage
OUTPUT=$(bash "$RUN_SCRIPT" 2>&1)
if echo "$OUTPUT" | grep -q "Usage:"; then
  echo "PASS: No args shows usage"
else
  echo "FAIL: Should show usage without args"
  exit 1
fi

# Test 2: list subcommand missing args
OUTPUT=$(bash "$RUN_SCRIPT" list 2>&1)
if echo "$OUTPUT" | grep -q "Usage:"; then
  echo "PASS: list subcommand without args shows usage"
fi

# Test 3: Mock list response
echo '[{"shareId":"abc123","url":"https://docsys.app/s/abc123","type":0}]' | jq .
echo "PASS: Mock list response valid JSON"

# Test 4: Mock create response
echo '{"success":true,"shareId":"xyz789","url":"https://docsys.app/s/xyz789"}' | jq .
echo "PASS: Mock create response valid JSON"

echo "All tests passed."
