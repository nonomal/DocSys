#!/bin/bash
# Skill: playwright
# Description: Manual browser automation via Playwright
# Usage: run.sh <operation> [args]
#   open <url>
#   screenshot [--full] [--path <path>]
#   click <selector>
#   fill <selector> <value>
#   close

set -e

CLI_URL="${DOCSYS_URL:-http://localhost:8080}"
CLI_USER="${DOCSYS_USER:-admin}"
CLI_PASS="${DOCSYS_PASS:-admin2026}"

OP="${1:-}"

if [ -z "$OP" ]; then
  echo "Usage: run.sh <operation> [args]"
  echo "  open <url>"
  echo "  screenshot [--full] [--path <path>]"
  echo "  click <selector>"
  echo "  fill <selector> <value>"
  echo "  close"
  exit 1
fi

ENDPOINT="/api/playwright"

case "$OP" in
  open)
    URL="${2:-}"
    if [ -z "$URL" ]; then
      echo "Usage: run.sh open <url>"
      exit 1
    fi
    PAYLOAD=$(jq -n --arg url "$URL" '{operation: "open", url: $url}')
    echo "Opening URL: $URL"
    ;;
  screenshot)
    FULL="false"
    PATH=""
    shift
    while [ $# -gt 0 ]; do
      case "$1" in
        --full) FULL="true"; shift ;;
        --path) PATH="$2"; shift 2 ;;
        *) shift ;;
      esac
    done
    PAYLOAD=$(jq -n --argjson full "$FULL" --arg path "$PATH" \
      '{operation: "screenshot", full: $full, path: $path}')
    echo "Taking screenshot (full: $FULL, path: $PATH)"
    ;;
  click)
    SEL="${2:-}"
    if [ -z "$SEL" ]; then
      echo "Usage: run.sh click <selector>"
      exit 1
    fi
    PAYLOAD=$(jq -n --arg sel "$SEL" '{operation: "click", selector: $sel}')
    echo "Clicking: $SEL"
    ;;
  fill)
    SEL="${2:-}"
    VAL="${3:-}"
    if [ -z "$SEL" ] || [ -z "$VAL" ]; then
      echo "Usage: run.sh fill <selector> <value>"
      exit 1
    fi
    PAYLOAD=$(jq -n --arg sel "$SEL" --arg val "$VAL" \
      '{operation: "fill", selector: $sel, value: $val}')
    echo "Filling: $SEL = $VAL"
    ;;
  close)
    PAYLOAD='{"operation":"close"}'
    echo "Closing browser"
    ;;
  *)
    echo "Unknown operation: $OP"
    exit 1
    ;;
esac

curl -s -X POST "${CLI_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -u "$CLI_USER:$CLI_PASS" \
  -d "$PAYLOAD" | jq .
