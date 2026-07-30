@echo off
rem Skill: lock_doc
rem Description: Lock a document to prevent editing by others
rem Usage: run.bat ^<vid^> ^<docId^> [type]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1
set DOC_ID=%2
set LOCK_TYPE=%3
if "%LOCK_TYPE%"=="" set LOCK_TYPE=1

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> [type]
  exit /b 1
)
if "%DOC_ID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> [type]
  exit /b 1
)

set ENDPOINT=/api/docs/%DOC_ID%/lock?vid=%VID%
set PAYLOAD={"type":%LOCK_TYPE%}

echo Locking document %DOC_ID% in repository VID: %VID% (type: %LOCK_TYPE%)
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
