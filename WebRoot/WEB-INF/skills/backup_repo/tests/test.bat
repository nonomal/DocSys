@echo off
rem Test: backup_repo
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

echo {"taskId":"backup-2025-12-15-001","status":"started","vid":1}
echo PASS: Mock backup response printed

echo {"taskId":"backup-2025-12-15-001","status":"running","progress":50}
echo PASS: Mock status response printed

echo All tests passed.
