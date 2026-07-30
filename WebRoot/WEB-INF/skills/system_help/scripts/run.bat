@echo off
rem Skill: system_help
rem Description: Show help and available commands
rem Usage: run.bat [command]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set COMMAND=%1

if not "%COMMAND%"=="" (
  set ENDPOINT=/api/help/%COMMAND%
  echo Getting help for command: %COMMAND%
) else (
  set ENDPOINT=/api/help
  echo Getting general help
)

curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
