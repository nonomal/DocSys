@echo off
rem Skill: rename_doc
rem Description: Rename a document or folder within a repository
rem Usage: run.bat ^<vid^> ^<docId^> ^<newName^>
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VID=%1
set DOC_ID=%2
set NEW_NAME=%3

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> ^<newName^>
  echo Example: run.bat 1 123 new_report.pdf
  exit /b 1
)

if "%DOC_ID%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> ^<newName^>
  exit /b 1
)

if "%NEW_NAME%"=="" (
  echo Usage: run.bat ^<vid^> ^<docId^> ^<newName^>
  exit /b 1
)

set ENDPOINT=/api/docs/%DOC_ID%?vid=%VID%
set PAYLOAD={"name":"%NEW_NAME%"}

echo Renaming document %DOC_ID% to '%NEW_NAME%' in repository VID: %VID%
curl -s -X PUT "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
