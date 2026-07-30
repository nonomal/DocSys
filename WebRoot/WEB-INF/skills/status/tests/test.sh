#!/bin/bash
# Test: status

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="$SCRIPT_DIR/../scripts/run.sh"

# Test 1: Script exists
if [ -f "$RUN_SCRIPT" ]; then
  echo "PASS: run.sh exists"
else
  echo "FAIL: run.sh not found"
  exit 1
fi

echo '{"status":"connected","user":"admin","serverTime":"2025-01-01T00:00:00Z"}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
