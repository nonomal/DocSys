# API Reference

## RAG Chat
- **Endpoint**: `POST /AI/rag.do`
- **Auth**: Required
- **Response**: JSON with AI answer and document citations

## Error Codes
| Code | Message |
|------|---------|
| 400 | Empty query |
| 401 | Unauthorized |
| 404 | No relevant documents found |
| 503 | AI service unavailable |
| 504 | Response timeout |
