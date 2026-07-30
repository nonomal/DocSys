# API Reference

## Rename Document
- **Endpoint**: `POST /Doc/rename.do`
- **Auth**: Required
- **Response**: JSON confirmation

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid name (forbidden chars) |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Document not found |
| 409 | Name conflict |
| 500 | Server error |
