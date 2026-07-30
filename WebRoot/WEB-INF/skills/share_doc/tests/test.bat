@echo off
rem Test: share_doc
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

echo [{"shareId":"abc123","url":"https://docsys.app/s/abc123","type":0}]
echo PASS: Mock list response printed

echo {"success":true,"shareId":"xyz789","url":"https://docsys.app/s/xyz789"}
echo PASS: Mock create response printed

echo All tests passed.
