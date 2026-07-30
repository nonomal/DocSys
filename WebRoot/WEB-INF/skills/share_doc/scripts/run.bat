@echo off
rem Skill: share_doc
rem Description: List or create document sharing links
rem Usage: run.bat list ^<vid^> ^<docId^>
rem        run.bat create ^<vid^> ^<docId^> [type] [password]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set ACTION=%1
set VID=%2
set DOC_ID=%3
set SHARE_TYPE=%4
set PASSWORD=%5

if "%ACTION%"=="" (
  echo Usage: run.bat list ^<vid^> ^<docId^>
  echo        run.bat create ^<vid^> ^<docId^> [type] [password]
  echo   type: 0=public, 1=password, 2=private
  exit /b 1
)

if "%ACTION%"=="list" (
  if "%VID%"=="" (
    echo Usage: run.bat list ^<vid^> ^<docId^>
    exit /b 1
  )
  if "%DOC_ID%"=="" (
    echo Usage: run.bat list ^<vid^> ^<docId^>
    exit /b 1
  )
  set ENDPOINT=/api/docs/%DOC_ID%/shares?vid=%VID%
  echo Listing share links for document %DOC_ID% in repository VID: %VID%
  curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
    -H "Content-Type: application/json" ^
    -u "%DOCSYS_USER%:%DOCSYS_PASS%"
  exit /b 0
)

if "%ACTION%"=="create" (
  if "%VID%"=="" (
    echo Usage: run.bat create ^<vid^> ^<docId^> [type] [password]
    exit /b 1
  )
  if "%DOC_ID%"=="" (
    echo Usage: run.bat create ^<vid^> ^<docId^> [type] [password]
    exit /b 1
  )
  if "%SHARE_TYPE%"=="" set SHARE_TYPE=0
  set ENDPOINT=/api/docs/%DOC_ID%/shares?vid=%VID%

  if not "%PASSWORD%"=="" (
    set PAYLOAD={"type":%SHARE_TYPE%,"password":"%PASSWORD%"}
  ) else (
    set PAYLOAD={"type":%SHARE_TYPE%}
  )

  echo Creating share link for document %DOC_ID% in repository VID: %VID%
  curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
    -H "Content-Type: application/json" ^
    -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
    -d "%PAYLOAD%"
  exit /b 0
)

echo Unknown action: %ACTION%
exit /b 1
