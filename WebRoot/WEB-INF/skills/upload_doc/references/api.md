# API Reference

## Upload Document
- **Endpoint**: `POST /Doc/upload.do`
- **Auth**: Required
- **Content-Type**: `multipart/form-data`
- **Response**: JSON with new document ID

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid request |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Repository not found |
| 413 | File too large |
| 500 | Server error |
