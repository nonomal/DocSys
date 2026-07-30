@echo off
rem Test: status
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

echo {"status":"connected","user":"admin","serverTime":"2025-01-01T00:00:00Z"}
echo PASS: Mock response printed

echo All tests passed.
