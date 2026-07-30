# API Reference

## Unlock Document
- **Endpoint**: `POST /Doc/unlock.do`
- **Auth**: Required
- **Response**: JSON confirmation

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid request |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Document not found |
| 409 | Not locked or locked by another user |
| 500 | Server error |
