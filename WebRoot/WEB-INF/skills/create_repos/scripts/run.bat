@echo off
rem Skill: create_repos
rem Description: Create a new repository in DocSystem
rem Usage: run.bat <name> <path>
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set NAME=%1
set PATH=%2

if "%NAME%"=="" (
  echo Usage: run.bat ^<name^> ^<path^>
  echo Example: run.bat MyProject F:\data\myrepo
  exit /b 1
)

if "%PATH%"=="" (
  echo Usage: run.bat ^<name^> ^<path^>
  echo Example: run.bat MyProject F:\data\myrepo
  exit /b 1
)

set ENDPOINT=/api/repos
set PAYLOAD={"name":"%NAME%","path":"%PATH%"}

echo Creating repository: %NAME% at %PATH%
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
