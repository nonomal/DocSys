@echo off
rem Skill: get_doc
rem Description: Get document details or preview content
rem Usage: run.bat ^<vid^> ^<docId^>
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1
set DOC_ID=%2

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^>
  echo Example: run.bat 1 123
  exit /b 1
)

if "%DOC_ID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^>
  exit /b 1
)

set ENDPOINT=/api/docs/%DOC_ID%?vid=%VID%

echo Getting document %DOC_ID% from repository VID: %VID%
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
