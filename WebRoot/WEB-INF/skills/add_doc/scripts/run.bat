@echo off
rem Skill: add_doc
rem Description: Create a new folder in a repository
rem Usage: run.bat ^<vid^> ^<name^>
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1
set NAME=%2

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^> ^<name^>
  echo Example: run.bat 1 reports
  exit /b 1
)

if "%NAME%"=="" (
  echo Usage: run.bat ^<vid^> ^<name^>
  echo Example: run.bat 1 reports
  exit /b 1
)

set ENDPOINT=/api/repos/%VID%/docs
set PAYLOAD={"name":"%NAME%","type":"folder"}

echo Creating folder '%NAME%' in repository VID: %VID%
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
