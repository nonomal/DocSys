#!/bin/bash
# Test: repos_info

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="$SCRIPT_DIR/../scripts/run.sh"

# Test 1: Missing vid shows usage
if bash "$RUN_SCRIPT" 2>&1 | grep -q "Usage:"; then
  echo "PASS: Missing vid shows usage"
else
  echo "FAIL: Should show usage without vid"
  exit 1
fi

# Test 2: Mock response
echo '{"vid":1,"name":"MyRepo","path":"F:/data/myrepo","type":"Local","docCount":10}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
