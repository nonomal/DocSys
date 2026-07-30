@echo off
rem Skill: backup_repos
rem Description: Start a repository backup and check its status
rem Usage: run.bat ^<vid^> [path]
rem        run.bat --status ^<task-id^>
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set ACTION=%1
set VID=%2
set TASK_PATH=%3

if "%ACTION%"=="--status" (
  if "%VID%"=="" (
    echo Usage: run.bat --status ^<task-id^>
    exit /b 1
  )
  set ENDPOINT=/api/repos/backup/status/%VID%
  echo Checking backup status for task: %VID%
  curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
    -H "Content-Type: application/json" ^
    -u "%DOCSYS_USER%:%DOCSYS_PASS%"
  exit /b 0
)

if "%VID%"=="" (
  echo Usage: run.bat ^<vid^> [path]
  echo        run.bat --status ^<task-id^>
  exit /b 1
)

if not "%TASK_PATH%"=="" (
  set PAYLOAD={"path":"%TASK_PATH%"}
) else (
  set PAYLOAD={}
)

set ENDPOINT=/api/repos/%VID%/backup
echo Starting backup for repository VID: %VID%
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
