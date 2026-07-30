@echo off
rem Skill: unlock_doc
rem Description: Unlock a document to allow editing
rem Usage: run.bat ^<vid^> ^<docId^>
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1
set DOC_ID=%2

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^>
  exit /b 1
)
if "%DOC_ID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^>
  exit /b 1
)

set ENDPOINT=/api/docs/%DOC_ID%/unlock?vid=%VID%

echo Unlocking document %DOC_ID% in repository VID: %VID%
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
