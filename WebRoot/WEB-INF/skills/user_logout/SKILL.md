---
name: user_logout
description: Logout from DocSystem and clear current session
category: user
version: 1.1.0
author: DocSys Team
permissions:
  - auth
tags: [auth, logout, session, security]
---

# User Logout

Logout from DocSystem and clear the current session.

## Triggers

Logout, 登出, 退出登录, sign out, 退出, 注销, log out, exit session, 退出系统, 登出系统

## Workflow

1. **Call CLI**: `docsys logout`
2. **Present result**: Confirm session cleared (session tokens, cached credentials).
3. **Already logged out → clean exit**: Return "Already logged out" without error code. If "Logout failed" → retry once; if still fails → check network and report.
4. **Post-logout note**: After successful logout, remind user to log in again if needed.

## CLI Command

```bash
docsys logout
```

## Parameters

None required.

## Examples

### Example 1
User: "logout"
Skill triggers → calls: `docsys logout`
Output: Session cleared. Tokens and cached credentials removed.

### Example 2
User: "登出"
Skill triggers → calls: `docsys logout`
Output: 会话已清理，凭证已移除

### Example 3
User: "sign me out"
Skill triggers → calls: `docsys logout`
Output: Logout successful. To log in again: `docsys login <username> <password>`

### Example 4 (already logged out)
User: "logout"
Skill triggers → calls: `docsys logout`
Output: Already logged out. No active session to clear. (Clean exit — no error code, no action needed.)

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| session | string | Session status: cleared / none |

**Success (EN):** `✅ Logged out. Session tokens and cached credentials cleared.`
**Success (中文):** `✅ 已退出登录。会话令牌和缓存凭据已清除。`
**Already logged out (EN):** `ℹ️ No active session. Nothing to log out.`
**Already logged out (中文):** `ℹ️ 当前无活跃会话。无需退出。`
**Error (EN):** `❌ Logout failed: {reason}. Retrying...`
**Error (中文):** `❌ 退出失败：{原因}。正在重试...`

## Error Handling

| Error | Cause |
|-------|-------|
| Already logged out | No active session — exit cleanly |
| Logout failed | API error — retry once, then report |
| Network error | Connection failed — check network, retry |

See [references/](references/) for related skills and API docs.
