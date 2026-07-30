@echo off
rem Skill: status
rem Description: Show DocSys connection status and session info
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set ENDPOINT=/api/status

echo Checking DocSys status...
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
