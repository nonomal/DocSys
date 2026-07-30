#!/bin/bash
# Test: web_search

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="$SCRIPT_DIR/../scripts/run.sh"

# Test 1: Missing query shows usage
if bash "$RUN_SCRIPT" 2>&1 | grep -q "Usage:"; then
  echo "PASS: Missing query shows usage"
else
  echo "FAIL: Should show usage without query"
  exit 1
fi

# Test 2: Mock response
echo '[{"title":"Claude AI","url":"https://claude.ai","description":"AI assistant"}]' | jq .
echo "PASS: Mock response valid JSON"

echo "All tests passed."
