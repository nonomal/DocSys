# API Reference

## Get Repository Info
- **Endpoint**: `GET /Repos/get.do`
- **Auth**: Required
- **Query Params**: `vid` — Repository ID
- **Response**: JSON with repository details

## Error Codes
| Code | Message |
|------|---------|
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Repository not found |
| 500 | Server error |
