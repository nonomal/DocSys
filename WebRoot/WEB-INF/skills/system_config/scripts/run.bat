@echo off
rem Skill: system_config
rem Description: Get or update DocSystem configuration (admin only)
rem Usage: run.bat [--get ^<key^>] [--set ^<key^> ^<value^>] [--list]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set ACTION=
set KEY=
set VALUE=

:parse_loop
if "%~1"=="" goto done_parse
if "%~1"=="--get" (
  set ACTION=get
  set KEY=%~2
  shift
  shift
  goto parse_loop
)
if "%~1"=="--set" (
  set ACTION=set
  set KEY=%~2
  set VALUE=%~3
  shift
  shift
  shift
  goto parse_loop
)
if "%~1"=="--list" (
  set ACTION=list
  shift
  goto parse_loop
)
shift
goto parse_loop

:done_parse

if "%ACTION%"=="list" (
  set ENDPOINT=/api/config?list=true
  echo Listing all configuration keys
  curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
    -H "Content-Type: application/json" ^
    -u "%DOCSYS_USER%:%DOCSYS_PASS%"
  exit /b 0
)

if "%ACTION%"=="get" (
  if "%KEY%"=="" (
    echo Usage: run.bat --get ^<key^>
    exit /b 1
  )
  set ENDPOINT=/api/config/%KEY%
  echo Getting config key: %KEY%
  curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
    -H "Content-Type: application/json" ^
    -u "%DOCSYS_USER%:%DOCSYS_PASS%"
  exit /b 0
)

if "%ACTION%"=="set" (
  if "%KEY%"=="" (
    echo Usage: run.bat --set ^<key^> ^<value^>
    exit /b 1
  )
  if "%VALUE%"=="" (
    echo Usage: run.bat --set ^<key^> ^<value^>
    exit /b 1
  )
  set ENDPOINT=/api/config
  set PAYLOAD={"key":"%KEY%","value":"%VALUE%"}
  echo Setting config key '%KEY%' = '%VALUE%'
  curl -s -X PUT "%DOCSYS_URL%%ENDPOINT%" ^
    -H "Content-Type: application/json" ^
    -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
    -d "%PAYLOAD%"
  exit /b 0
)

set ENDPOINT=/api/config
echo Getting all configuration
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
