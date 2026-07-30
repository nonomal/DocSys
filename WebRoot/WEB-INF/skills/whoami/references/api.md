# API Reference

## Get Current User
- **Endpoint**: `GET /User/getLoginUser.do`
- **Auth**: Required
- **Response**: JSON with username, role, permissions, login_time, session_status

## Error Codes
| Code | Message |
|------|---------|
| 401 | Not logged in |
| 403 | Session expired |
| 500 | Server error |
