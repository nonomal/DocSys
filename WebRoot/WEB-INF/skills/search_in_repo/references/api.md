# API Reference

## Search In Repository
- **Endpoint**: `POST /Query/search.do`
- **Auth**: Required
- **Response**: JSON array of matching documents within the specified repository

## Error Codes
| Code | Message |
|------|---------|
| 400 | Empty query |
| 401 | Unauthorized |
| 404 | Repository not found |
| 500 | Server error |
