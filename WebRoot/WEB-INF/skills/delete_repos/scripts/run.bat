@echo off
rem Skill: delete_repos
rem Description: Delete a repository from DocSystem
rem Usage: run.bat ^<vid^>
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^>
  echo Example: run.bat 5
  exit /b 1
)

set ENDPOINT=/api/repos/%VID%

echo Deleting repository VID: %VID%
curl -s -X DELETE "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
