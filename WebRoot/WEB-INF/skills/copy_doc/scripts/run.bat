@echo off
rem Skill: copy_doc
rem Description: Copy a document to another folder within the same repository
rem Usage: run.bat ^<vid^> ^<docId^> ^<targetPid^>
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1
set DOC_ID=%2
set TARGET_PID=%3

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> ^<targetPid^>
  exit /b 1
)
if "%DOC_ID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> ^<targetPid^>
  exit /b 1
)
if "%TARGET_PID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> ^<targetPid^>
  exit /b 1
)

set ENDPOINT=/api/docs/%DOC_ID%/copy?vid=%VID%
set PAYLOAD={"targetPid":%TARGET_PID%}

echo Copying document %DOC_ID% to folder %TARGET_PID% in repository VID: %VID%
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
