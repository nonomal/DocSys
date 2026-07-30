# API Reference

## Lock Document
- **Endpoint**: `POST /Doc/lock.do`
- **Auth**: Required
- **Response**: JSON confirmation

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid request |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Document not found |
| 409 | Already locked |
| 500 | Server error |
