@echo off
rem Skill: download_doc
rem Description: Download a document to local filesystem
rem Usage: run.bat ^<vid^> ^<docId^> [localPath]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1
set DOC_ID=%2
set LOCAL_PATH=%3
if "%LOCAL_PATH%"=="" set LOCAL_PATH=.

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> [localPath]
  exit /b 1
)

if "%DOC_ID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> [localPath]
  exit /b 1
)

set ENDPOINT=/api/docs/%DOC_ID%/download?vid=%VID%

echo Downloading document %DOC_ID% from repository VID: %VID%
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -o "%LOCAL_PATH%\document_%DOC_ID%"
echo Downloaded to: %LOCAL_PATH%\document_%DOC_ID%
