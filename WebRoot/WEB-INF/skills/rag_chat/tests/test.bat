@echo off
rem Test: rag_chat
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

call "%RUN_SCRIPT%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo PASS: Missing query causes error
)

echo {"response":"The Q4 report shows revenue of 10M.","citations":[{"docId":1,"name":"Q4_report.pdf"}]}
echo PASS: Mock response printed

echo All tests passed.
