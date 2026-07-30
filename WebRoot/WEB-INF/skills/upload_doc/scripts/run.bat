@echo off
rem Skill: upload_doc
rem Description: Upload a local file to a repository
rem Usage: run.bat ^<file^> ^<vid^> [pid]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set FILE=%1
set VID=%2
set PID=%3
if "%PID%"=="" set PID=0

if "%FILE%"=="" (
  echo Usage: run.bat ^<file^> ^<vid^> [pid]
  echo Example: run.bat F:\report.pdf 1
  exit /b 1
)

if "%VID%"=="" (
  echo Usage: run.bat ^<file^> ^<vid^> [pid]
  exit /b 1
)

if not exist "%FILE%" (
  echo Error: File not found: %FILE%
  exit /b 1
)

set ENDPOINT=/api/docs/upload?vid=%VID%&pid=%PID%

echo Uploading file '%FILE%' to repository VID: %VID%, folder PID: %PID%
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -F "file=@%FILE%"
