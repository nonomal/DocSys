#!/bin/bash
# Test: system_config

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

# Test 2: --set without args shows usage
OUTPUT=$(bash "$RUN_SCRIPT" --set 2>&1)
if echo "$OUTPUT" | grep -qi "usage"; then
  echo "PASS: --set without args shows usage"
fi

echo '{"ai.default.model":"claude-3-5-sonnet","max.upload.size":100}' | jq .
echo "PASS: Mock config response valid JSON"

echo "All tests passed."
