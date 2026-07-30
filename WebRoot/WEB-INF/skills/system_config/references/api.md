# API Reference

## Get/Update System Config
- **Endpoint**: `GET /System/config.do`
- **Auth**: Required (admin)
- **Response**: JSON with configuration key-value pairs

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid key or value |
| 401 | Unauthorized |
| 403 | Permission denied |
| 500 | Server error |
