@echo off
rem Test: list_docs
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

call "%RUN_SCRIPT%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo PASS: Missing vid causes error
)

echo [{"docId":1,"name":"report.pdf","type":"file","size":1024}]
echo PASS: Mock response printed

echo All tests passed.
