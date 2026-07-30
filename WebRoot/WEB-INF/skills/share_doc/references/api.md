# API Reference

## List Share Links
- **Endpoint**: `GET /DocShare/list.do`
- **Auth**: Required
- **Response**: JSON array of share links

## Create Share Link
- **Endpoint**: `POST /DocShare/create.do`
- **Auth**: Required
- **Response**: JSON with new share URL

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid request |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Document not found |
| 500 | Server error |
