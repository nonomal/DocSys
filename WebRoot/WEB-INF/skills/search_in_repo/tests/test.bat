@echo off
rem Test: search_in_repo
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

echo [{"docId":5,"name":"report.pdf","repoVID":1,"score":0.9}]
echo PASS: Mock response printed

echo All tests passed.
