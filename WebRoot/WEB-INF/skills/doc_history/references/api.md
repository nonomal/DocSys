# API Reference

## Get Document History
- **Endpoint**: `GET /Doc/history.do`
- **Auth**: Required
- **Response**: JSON array of document versions (newest first)

## Error Codes
| Code | Message |
|------|---------|
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Document not found |
| 500 | Server error |
