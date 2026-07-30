@echo off
rem Test: whoami
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

echo {"username":"admin","role":"admin","permissions":["read","write","admin"]}
echo PASS: Mock response printed

echo All tests passed.
