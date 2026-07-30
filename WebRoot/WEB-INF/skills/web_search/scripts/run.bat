@echo off
rem Skill: web_search
rem Description: Search the web using search engines (Baidu, Google, Bing)
rem Usage: run.bat ^<query^> [--engine baidu] [--limit 10]
setlocal enabledelayedexpansion

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set QUERY=
set ENGINE=baidu
set LIMIT=10

:parse_loop
if "%~1"=="" goto done_parse
if "%~1"=="--engine" (
  set ENGINE=%~2
  shift
  shift
  goto parse_loop
)
if "%~1"=="--limit" (
  set LIMIT=%~2
  shift
  shift
  goto parse_loop
)
set QUERY=%~1
shift
goto parse_loop

:done_parse
if "%QUERY%"=="" (
  echo Usage: run.bat ^<query^> [--engine baidu] [--limit 10]
  exit /b 1
)

set "QUERY_ESCAPED=%QUERY: =+%"

set ENDPOINT=/api/web-search?q=%QUERY_ESCAPED%&engine=%ENGINE%&limit=%LIMIT%

echo Web search for: %QUERY% (engine: %ENGINE%, limit: %LIMIT%)
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
