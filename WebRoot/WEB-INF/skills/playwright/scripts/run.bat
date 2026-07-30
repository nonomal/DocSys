@echo off
rem Skill: playwright
rem Description: Manual browser automation via Playwright
rem Usage: run.bat ^<operation^> [args]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set OP=%1

if "%OP%"=="" (
  echo Usage: run.bat ^<operation^> [args]
  exit /b 1
)

set ENDPOINT=/api/playwright

if "%OP%"=="open" (
  set URL=%2
  if "%URL%"=="" (
    echo Usage: run.bat open ^<url^>
    exit /b 1
  )
  set PAYLOAD={"operation":"open","url":"%URL%"}
  echo Opening URL: %URL%
  goto run_cmd
)

if "%OP%"=="screenshot" (
  set FULL=false
  set SCREENSHOT_PATH=
  :screenshot_loop
  if "%~2"=="" goto done_screenshot
  if "%~2"=="--full" (
    set FULL=true
    shift
    shift
    goto screenshot_loop
  )
  if "%~2"=="--path" (
    set SCREENSHOT_PATH=%~3
    shift
    shift
    shift
    goto screenshot_loop
  )
  shift
  goto screenshot_loop
  :done_screenshot
  set PAYLOAD={"operation":"screenshot","full":%FULL%,"path":"%SCREENSHOT_PATH%"}
  echo Taking screenshot
  goto run_cmd
)

if "%OP%"=="click" (
  set SEL=%2
  if "%SEL%"=="" (
    echo Usage: run.bat click ^<selector^>
    exit /b 1
  )
  set PAYLOAD={"operation":"click","selector":"%SEL%"}
  echo Clicking: %SEL%
  goto run_cmd
)

if "%OP%"=="fill" (
  set SEL=%2
  set VAL=%3
  if "%SEL%"=="" (
    echo Usage: run.bat fill ^<selector^> ^<value^>
    exit /b 1
  )
  if "%VAL%"=="" (
    echo Usage: run.bat fill ^<selector^> ^<value^>
    exit /b 1
  )
  set PAYLOAD={"operation":"fill","selector":"%SEL%","value":"%VAL%"}
  echo Filling: %SEL% = %VAL%
  goto run_cmd
)

if "%OP%"=="close" (
  set PAYLOAD={"operation":"close"}
  echo Closing browser
  goto run_cmd
)

echo Unknown operation: %OP%
exit /b 1

:run_cmd
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
