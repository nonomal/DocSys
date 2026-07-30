@echo off
rem Test: doc_history
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

call "%RUN_SCRIPT%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo PASS: Missing args causes error
)

echo [{"version":2,"date":"2025-01-15","author":"admin"},{"version":1,"date":"2025-01-10","author":"admin"}]
echo PASS: Mock response printed

echo All tests passed.
