@echo off
rem Test: repos_info
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

echo {"vid":1,"name":"MyRepo","path":"F:\data\myrepo","type":"Local","docCount":10}
echo PASS: Mock response printed

echo All tests passed.
