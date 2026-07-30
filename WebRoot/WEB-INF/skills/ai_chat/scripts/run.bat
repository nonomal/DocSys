@echo off
rem Skill: ai_chat
rem Description: Chat with AI assistant for general questions and conversations
rem Usage: run.bat ^<message^> [model]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set MESSAGE=%1
set MODEL=%2

if "%MESSAGE%"=="" (
  echo Usage: run.bat ^<message^> [model]
  echo Example: run.bat "what is DocSystem?"
  exit /b 1
)

set ENDPOINT=/api/chat

if not "%MODEL%"=="" (
  set PAYLOAD={"message":"%MESSAGE%","model":"%MODEL%"}
) else (
  set PAYLOAD={"message":"%MESSAGE%"}
)

echo Sending chat message...
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
