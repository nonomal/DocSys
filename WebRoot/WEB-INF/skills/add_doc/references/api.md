# API Reference

## Add Document/Folder
- **Endpoint**: `POST /Doc/add.do`
- **Auth**: Required
- **Response**: JSON with new folder ID

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid name (forbidden chars) |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Repository not found |
| 409 | Folder already exists |
| 500 | Server error |
