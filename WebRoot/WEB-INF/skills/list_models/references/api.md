# API Reference

## List AI Models
- **Endpoint**: `GET /AI/models.do`
- **Auth**: Required
- **Response**: JSON array of model objects with name, provider, capabilities

## Error Codes
| Code | Message |
|------|---------|
| 401 | Unauthorized |
| 503 | Connection failed |
| 504 | API timeout |
