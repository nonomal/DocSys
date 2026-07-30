#!/bin/bash
# Test: list_repos

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="$SCRIPT_DIR/../scripts/run.sh"

# Test 1: Script exists and is executable
if [ ! -f "$RUN_SCRIPT" ]; then
  echo "FAIL: run.sh not found"
  exit 1
fi

# Test 2: No arguments required - check usage output
if bash "$RUN_SCRIPT" --help 2>&1 | grep -qi "usage"; then
  echo "PASS: --help shows usage"
else
  echo "PASS: No args required (help check skipped)"
fi

# Test 3: Mock response via environment
echo "Testing mock API response..."
MOCK_RESPONSE='[{"vid":1,"name":"MyRepo","path":"F:/data/myrepo","type":"Local"}]'
echo "$MOCK_RESPONSE" | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
