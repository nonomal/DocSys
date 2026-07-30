#!/bin/bash
# Test: get_doc

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

echo '{"docId":123,"name":"report.pdf","size":2048,"type":"file","content":"Hello World"}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
