@echo off
rem Skill: rag_chat
rem Description: Chat with AI using relevant documents as context (RAG)
rem Usage: run.bat ^<query^> [model]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set QUERY=%1
set MODEL=%2

if "%QUERY%"=="" (
  echo Usage: run.bat ^<query^> [model]
  echo Example: run.bat "what does the Q4 report say?"
  exit /b 1
)

set ENDPOINT=/api/rag

if not "%MODEL%"=="" (
  set PAYLOAD={"query":"%QUERY%","model":"%MODEL%"}
) else (
  set PAYLOAD={"query":"%QUERY%"}
)

echo Sending RAG query...
curl -s -X POST "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%" ^
  -d "%PAYLOAD%"
