# API Reference

## Get Document
- **Endpoint**: `GET /Doc/get.do`
- **Auth**: Required
- **Response**: JSON with document metadata and content (text files) or metadata only (binary)

## Error Codes
| Code | Message |
|------|---------|
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Document not found |
| 500 | Server error |
