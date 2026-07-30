# API Reference

## Delete Repository
- **Endpoint**: `POST /Repos/delete.do`
- **Auth**: Required (admin)
- **Response**: JSON confirmation

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid request |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Repository not found |
| 500 | Server error |
