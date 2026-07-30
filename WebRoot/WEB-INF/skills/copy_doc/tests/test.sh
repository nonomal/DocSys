#!/bin/bash
# Test: copy_doc

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

echo '{"success":true,"newDocId":456,"docId":123,"targetPid":5}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
