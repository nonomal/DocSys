@echo off
rem Skill: banner
rem Description: Display the DocSys welcome banner
rem Usage: run.bat [name]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set NAME=%1

if not "%NAME%"=="" (
  set ENDPOINT=/api/banner/%NAME%
  echo Displaying banner: %NAME%
) else (
  set ENDPOINT=/api/banner
  echo Displaying default welcome banner
)

curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
