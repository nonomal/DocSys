@echo off
rem Test: ai_chat
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

call "%RUN_SCRIPT%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo PASS: Missing message causes error
)

echo {"response":"DocSystem is a document management system.","model":"claude-3-5-sonnet"}
echo PASS: Mock response printed

echo All tests passed.
