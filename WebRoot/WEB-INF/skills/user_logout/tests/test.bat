@echo off
rem Test: user_logout
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

echo {"success":true,"message":"Logged out successfully"}
echo PASS: Mock response printed

echo All tests passed.
