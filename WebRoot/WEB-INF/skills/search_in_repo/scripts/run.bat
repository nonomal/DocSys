@echo off
rem Skill: search_in_repo
rem Description: Full-text search within a specific repository
rem Usage: run.bat ^<query^> ^<vid^>
setlocal enabledelayedexpansion

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set QUERY=%1
set VID=%2

if "%QUERY%"=="" (
  echo Usage: run.bat ^<query^> ^<vid^>
  exit /b 1
)

if "%VID%"=="" (
  echo Usage: run.bat ^<query^> ^<vid^>
  exit /b 1
)

set "QUERY_ESCAPED=%QUERY: =+%"

set ENDPOINT=/api/search?q=%QUERY_ESCAPED%&vid=%VID%

echo Searching for '%QUERY%' in repository VID: %VID%
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
