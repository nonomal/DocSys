# API Reference

## AI Chat
- **Endpoint**: `POST /AI/chat.do`
- **Auth**: Required
- **Response**: JSON with AI response text

## Error Codes
| Code | Message |
|------|---------|
| 400 | Empty message |
| 401 | Unauthorized |
| 404 | Model not found |
| 503 | AI service unavailable |
| 504 | Response timeout |
