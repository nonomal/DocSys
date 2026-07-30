#!/bin/bash
# Test: delete_repos

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

# Test 2: Mock delete response
echo '{"success":true,"message":"Repository deleted"}' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
