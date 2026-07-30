@echo off
rem Skill: list_models
rem Description: List all available AI models for chat and RAG
rem Usage: run.bat [--verbose]
setlocal

set DOCSYS_URL=http://localhost:8080
set DOCSYS_USER=admin
set DOCSYS_PASS=admin2026

set VERBOSE=false
if "%~1"=="--verbose" (
  set VERBOSE=true
)

set ENDPOINT=/api/models?verbose=%VERBOSE%

echo Listing available AI models (verbose: %VERBOSE%)
curl -s -X GET "%DOCSYS_URL%%ENDPOINT%" ^
  -H "Content-Type: application/json" ^
  -u "%DOCSYS_USER%:%DOCSYS_PASS%"
