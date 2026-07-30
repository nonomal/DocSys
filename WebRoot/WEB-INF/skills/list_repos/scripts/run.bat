@echo off
rem Skill: list_repos
rem Description: List all accessible repositories in DocSystem
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set ENDPOINT=/api/repos

echo Calling: %ENDPOINT%
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
