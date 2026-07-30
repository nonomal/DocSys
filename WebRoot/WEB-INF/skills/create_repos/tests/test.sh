#!/bin/bash
# Test: create_repos

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

# Test 2: Missing second arg shows usage
if bash "$RUN_SCRIPT" "MyRepo" 2>&1 | grep -q "Usage:"; then
  echo "PASS: Missing second arg shows usage"
else
  echo "FAIL: Should show usage without path"
  exit 1
fi

# Test 3: Mock payload validation
MOCK_PAYLOAD='{"name":"TestRepo","path":"F:/data/test"}'
echo "$MOCK_PAYLOAD" | jq .name > /dev/null && echo "PASS: Valid mock payload"

echo "All tests passed."
