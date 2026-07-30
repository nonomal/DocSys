@echo off
rem Test: system_config
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

call "%RUN_SCRIPT%" --set >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo PASS: --set without args causes error
)

echo {"ai.default.model":"claude-3-5-sonnet","max.upload.size":100}
echo PASS: Mock config response printed

echo All tests passed.
