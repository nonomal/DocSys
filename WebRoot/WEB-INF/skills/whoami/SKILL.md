---
name: whoami
description: Show current user info and session status
category: user
version: 1.1.0
author: DocSys Team
permissions:
  - read
tags: [user, identity, session, auth]
---

# Who Am I

Display current user information and session status.

## Triggers

Whoami, 我是谁, 当前用户, 我的信息, user info, current user, who am I, user details, 个人信息, 账户信息, 用户信息, 当前登录账号, 查看账户, 登录状态, am I logged in, what is my username, my account, 登录信息, 我的账号, 当前账号

## Workflow

1. **Call CLI**: `docsys whoami` — no parameters required
2. **Present result**: Format as key-value pairs: username, role, permissions, login_time, session_status.
3. **Error handling**: If "Not logged in" → suggest running `docsys login`. If "Session expired" → suggest re-authenticating.

## CLI Command

```bash
docsys whoami
```

## Parameters

None required.

## Examples

### Example 1 (正常登录)
User: "who am I"
Skill triggers → calls: `docsys whoami`
Output: username: admin, role: admin, login_time: 2025-12-15 09:00:00, session: active

### Example 2 (中文场景)
User: "当前登录账号是什么"
Skill triggers → calls: `docsys whoami`
Output: 当前用户：admin，角色：管理员，登录时间：2025-12-15 09:00

### Example 3 (错误场景)
User: "am I logged in?"
Skill triggers → calls: `docsys whoami`
Output: 错误：Not logged in（未登录时返回此错误）

### Example 4 (会话过期)
User: "我的登录状态"
Skill triggers → calls: `docsys whoami`
Output: 错误：Session expired（会话过期时返回此错误）

## Output Format

Success (English):
```
User: <username>
Role: <admin | user | guest>
Permissions: <read | write | admin>
Login Time: <YYYY-MM-DD HH:MM>
Session Status: <active | expiring-soon | expired>
Token Scope: <token_scope>
```

Success (中文):
```
用户名: <username>
角色: <管理员 | 普通用户 | 访客>
权限: <只读 | 可写 | 管理员>
登录时间: <YYYY-MM-DD HH:MM>
会话状态: <活跃 | 即将过期 | 已过期>
Token 范围: <token_scope>
```

| Field | English | 中文 |
|-------|---------|------|
| username | "User: admin" | "用户名: admin" |
| role | "Role: admin" | "角色: 管理员" |
| permissions | "Permissions: read,write" | "权限: 只读,可写" |
| login_time | "Login Time: 2025-12-15 09:00" | "登录时间: 2025-12-15 09:00" |
| session_status | "Session: active" | "会话状态: 活跃" |
| token_scope | "Token Scope: full-access" | "Token 范围: full-access" |

Error: "Not logged in" (no session → suggest `docsys login`), "Session expired" (token invalid → suggest `docsys login`), "Token invalid" (corrupted → suggest `docsys login` to re-authenticate)

## Error Handling

| Error | Cause |
|-------|-------|
| Not logged in | No active session |
| Session expired | Token invalid or expired |
| Token invalid | Authentication token corrupted |

See [references/](references/) for related skills and API docs.
