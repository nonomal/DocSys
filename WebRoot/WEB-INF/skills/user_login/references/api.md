# API Reference

## User Login
- **Endpoint**: `POST /User/login.do`
- **Auth**: N/A (login endpoint)
- **Response**: JSON with user info and session token

## Error Codes
| Code | Message |
|------|---------|
| 400 | Missing username or password |
| 401 | Invalid credentials |
| 403 | Account locked or disabled |
| 500 | Server error |
