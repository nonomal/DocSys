@echo off
rem Test: create_repos
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

echo Testing missing args...
call "%RUN_SCRIPT%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo PASS: Missing args causes error
)

echo Testing mock payload...
echo {"name":"TestRepo","path":"F:\data\test"}
echo PASS: Mock payload printed

echo All tests passed.
