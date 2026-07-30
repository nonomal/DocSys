---
name: status
description: Show DocSys connection status and session info
category: system
version: 1.1.0
author: DocSys Team
permissions:
  - read
tags: [system, status, connection, session, info]
---

# Status

Show DocSys connection status, login state, and current user info.

## Triggers

Status, 状态, 连接状态, session, 会话状态, connection status, am I logged in, am I connected, server status, 服务状态, 连接情况

## Workflow

1. **Call CLI**: `docsys status` — no parameters required.
2. **Parse response**: Extract: base URL, connection status, logged-in user, server time.
3. **Format output**: Present as key-value pairs. Match language to query (Chinese query → Chinese output).
4. **Error recovery**: If "Cannot connect" → check server URL in config, verify network. If "Session expired" → suggest running `docsys login`.

## CLI Command

```bash
docsys status
```

## Parameters

None required.

## Examples

### Example 1
User: "what is the connection status"
Skill triggers → calls: `docsys status`
Output: Base URL, connection status, logged-in user

### Example 2
User: "show status"
Skill triggers → calls: `docsys status`
Output: Status information

### Example 3
User: "当前连接正常吗"
Skill triggers → calls: `docsys status`
Output: 连接状态：服务器地址、连接状态（已连接/未连接）、当前用户、服务器时间

### Example 3
User: "连接状态"
Skill triggers → calls: `docsys status`
Output: 连接状态信息

## Output Format

Success (English): Base URL, Connection (connected/disconnected), Logged-in User (username + role), Server Time.
Success (中文): 服务器地址 (base URL), 连接状态 (connected/disconnected), 当前用户 (username + role), 服务器时间 (server time).

| Field | English Output | 中文输出 |
|-------|---------------|---------|
| Base URL | "Base URL: https://..." | "服务器地址: https://..." |
| Connection | "Status: Connected / Disconnected" | "连接状态: 已连接 / 未连接" |
| User | "User: username (role)" | "当前用户: username (角色)" |
| Server Time | "Server time: YYYY-MM-DD HH:mm:ss" | "服务器时间: YYYY-MM-DD HH:mm:ss" |

## Error Handling

| Error | Cause |
|-------|-------|
| Cannot connect | Server unreachable |
| Session expired | Token invalid |

See [references/](references/) for related skills and API docs.
