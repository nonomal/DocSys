# API Reference

## Search Documents
- **Endpoint**: `POST /Query/search.do`
- **Auth**: Required
- **Response**: JSON array of matching documents

## Error Codes
| Code | Message |
|------|---------|
| 400 | Empty query |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Repository not found (when vid provided) |
| 500 | Server error |
