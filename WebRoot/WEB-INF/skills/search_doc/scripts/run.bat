@echo off
rem Skill: search_doc
rem Description: Full-text search across all repositories
rem Usage: run.bat ^<query^> [vid]
setlocal enabledelayedexpansion

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set QUERY=%1
set VID=%2

if "%QUERY%"=="" (
  echo Usage: run.bat ^<query^> [vid]
  echo Example: run.bat project
  exit /b 1
)

set "QUERY_ESCAPED=%QUERY: =+%"

if not "%VID%"=="" (
  set ENDPOINT=/api/search?q=%QUERY_ESCAPED%&vid=%VID%
  echo Searching for: %QUERY% in repository VID: %VID%
) else (
  set ENDPOINT=/api/search?q=%QUERY_ESCAPED%
  echo Searching for: %QUERY%
)

curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
