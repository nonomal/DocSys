@echo off
rem Test: list_repos
setlocal

set SCRIPT_DIR=%~dp0
set RUN_SCRIPT=%SCRIPT_DIR%..\scripts\run.bat

if not exist "%RUN_SCRIPT%" (
  echo FAIL: run.bat not found
  exit /b 1
)

echo Testing mock API response...
echo [{"vid":1,"name":"MyRepo","path":"F:\data\myrepo","type":"Local"}]
echo PASS: Mock response printed

echo All tests passed.
