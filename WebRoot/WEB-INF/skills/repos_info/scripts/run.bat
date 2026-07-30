@echo off
rem Skill: repos_info
rem Description: Get detailed information about a specific repository
rem Usage: run.bat ^<vid^>
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^>
  echo Example: run.bat 1
  exit /b 1
)

set ENDPOINT=/api/repos/%VID%

echo Getting repository info for VID: %VID%
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
