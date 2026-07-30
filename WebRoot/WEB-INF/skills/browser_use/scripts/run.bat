@echo off
rem Skill: browser_use
rem Description: AI-powered web browsing via browser-use.com API
rem Usage: run.bat ^<task^> [--url ^<url^>] [--max-steps N]
setlocal enabledelayedexpansion

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set TASK=
set URL=
set MAX_STEPS=10

:parse_loop
if "%~1"=="" goto done_parse
if "%~1"=="--url" (
  set URL=%~2
  shift
  shift
  goto parse_loop
)
if "%~1"=="--max-steps" (
  set MAX_STEPS=%~2
  shift
  shift
  goto parse_loop
)
set TASK=%~1
shift
goto parse_loop

:done_parse
if "%TASK%"=="" (
  echo Usage: run.bat ^<task^> [--url ^<url^>] [--max-steps N]
  exit /b 1
)

set ENDPOINT=/api/browser-use

if not "%URL%"=="" (
  set PAYLOAD={"task":"%TASK%","url":"%URL%","maxSteps":%MAX_STEPS%}
) else (
  set PAYLOAD={"task":"%TASK%","maxSteps":%MAX_STEPS%}
)

echo Running AI browser task: %TASK%
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
