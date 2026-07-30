# API Reference

## Start Backup
- **Endpoint**: `POST /Repos/backup.do`
- **Auth**: Required (admin)
- **Response**: JSON with task-id

## Check Backup Status
- **Endpoint**: `GET /Repos/backup/status.do`
- **Auth**: Required
- **Query Params**: `taskId` — Backup task ID
- **Response**: JSON with progress and status

## Error Codes
| Code | Message |
|------|---------|
| 400 | Invalid request |
| 401 | Unauthorized |
| 403 | Permission denied |
| 404 | Repository not found |
| 409 | Backup already running |
| 500 | Server error |
