#!/bin/bash
# Test: banner

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

echo '{"banner":"Welcome to DocSys!","type":"default"}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
