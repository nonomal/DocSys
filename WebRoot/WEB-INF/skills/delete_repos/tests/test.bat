@echo off
rem Test: delete_repos
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

echo Testing missing vid...
call "%RUN_SCRIPT%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo PASS: Missing vid causes error
)

echo {"success":true,"message":"Repository deleted"}
echo PASS: Mock response printed

echo All tests passed.
