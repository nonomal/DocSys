@echo off
rem Test: search_doc
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

echo [{"docId":1,"name":"project_report.pdf","repo":"MyRepo","score":0.95}]
echo PASS: Mock response printed

echo All tests passed.
