# API Reference

## Create Repository
- **Endpoint**: `POST /Repos/add.do`
- **Auth**: Required
- **Response**: JSON with new repository VID

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid request (missing name/path) |
| 401 | Unauthorized |
| 403 | Permission denied |
| 409 | Repository already exists |
| 500 | Server error |
