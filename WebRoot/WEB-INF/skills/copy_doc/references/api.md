# API Reference

## Copy Document
- **Endpoint**: `POST /Doc/copy.do`
- **Auth**: Required
- **Response**: JSON with new document ID

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid request |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Document or target folder not found |
| 507 | Insufficient storage |
| 500 | Server error |
