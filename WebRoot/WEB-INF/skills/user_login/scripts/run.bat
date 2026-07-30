@echo off
rem Skill: user_login
rem Description: Login to DocSystem with username and password
rem Usage: run.bat ^<username^> ^<password^>
setlocal

set DOCSYS_URL=http://localhost:8080

set USERNAME=%1
set PASSWORD=%2

if "%USERNAME%"=="" (
  echo Usage: run.bat ^<username^> ^<password^>
  echo Example: run.bat admin admin2026
  exit /b 1
)

if "%PASSWORD%"=="" (
  echo Usage: run.bat ^<username^> ^<password^>
  exit /b 1
)

set ENDPOINT=/api/auth/login
set PAYLOAD={"username":"%USERNAME%","password":"%PASSWORD%"}

echo Logging in as: %USERNAME%
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -d "%PAYLOAD%"
