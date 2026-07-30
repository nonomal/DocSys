#!/bin/bash
# Test: doc_history

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

echo '[{"version":2,"date":"2025-01-15","author":"admin"},{"version":1,"date":"2025-01-10","author":"admin"}]' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
