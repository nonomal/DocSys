@echo off
rem Skill: list_docs
rem Description: List documents and folders in a repository or folder
rem Usage: run.bat ^<vid^> [pid]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1
set PID=%2
if "%PID%"=="" set PID=0

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^> [pid]
  echo Example: run.bat 1
  exit /b 1
)

set ENDPOINT=/api/repos/%VID%/docs?pid=%PID%

echo Listing documents in repository VID: %VID%, folder PID: %PID%
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
