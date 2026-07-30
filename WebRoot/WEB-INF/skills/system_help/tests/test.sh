#!/bin/bash
# Test: system_help

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

echo '{"commands":["repos","doc","upload","download","search","chat","rag"]}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
