---
name: user_login
description: Login to DocSystem with username and password
category: user
version: 1.1.0
author: DocSys Team
permissions:
  - auth
tags: [auth, login, session, security, credential]
---

# User Login

Login to DocSystem with username and password.

## Triggers

Login, 登录, 用户登录, sign in, 登入, 登录系统, log in, authenticate, 账号登录, 进入系统, log me in

## Workflow

1. **Parse credentials**: Extract `username` and `password` from user input.
2. **Missing password → prompt**: If password not provided → ask user for it. **Do not call the CLI with missing arguments.** If user declines → stop.
3. **Call CLI**: `docsys login <username> <password>`
4. **Present result**: Return username, role, session status. **Never display the password in the response.**
5. **Error handling**: Map errors to user-friendly messages. Use generic "Invalid credentials" (not "wrong password" or "wrong username") to prevent enumeration.

## CLI Command

```bash
docsys login <username> <password>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| username | string | yes | Username |
| password | string | yes | Password |

## Examples

### Example 1
User: "login as admin with password password123"
Skill triggers → calls: `docsys login admin password123`
Output: Login success with user info

### Example 2
User: "登录 zhangsan mypassword"
Skill triggers → calls: `docsys login zhangsan mypassword`
Output: 登录结果

### Example 3 (missing password → prompts)
User: "log me in as admin"
Skill triggers → Step 1 extracts username=admin; Step 2 detects password is missing → prompts user: "Please enter your password:" → user supplies password → calls: `docsys login admin <password>`
Output: Login success with username, role, session status. **Password never echoed.**

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| username | string | Authenticated username |
| role | string | User role (e.g., admin, user) |
| session | string | Session status: active / expired |
| logged_in_at | string | Login timestamp |

**Success (EN):** `✅ Logged in as {username} (role: {role}) | Session: active | Logged in at: {timestamp}`
**Success (中文):** `✅ 登录成功: {username}（角色: {role}）| 会话: active | 登录时间: {timestamp}`
**Error (EN):** `❌ Login failed: Invalid credentials. Check your username and password.`
**Error (中文):** `❌ 登录失败：用户名或密码错误。`
**Account locked (EN):** `❌ Account locked due to too many failed attempts. Try again in 15 minutes or contact admin.`
**Account locked (中文):** `❌ 账户因多次登录失败已被锁定。请 15 分钟后再试，或联系管理员。`
**Account disabled (EN):** `❌ Account disabled. Contact admin to re-enable.`
**Account disabled (中文):** `❌ 账户已被禁用。请联系管理员重新启用。`
**Connection failed (EN):** `❌ Cannot connect to DocSystem server. Check network and try again.`
**Connection failed (中文):** `❌ 无法连接到 DocSystem 服务器。请检查网络连接。`
**Missing args (EN):** `Usage: login <username> <password>`
**Missing args (中文):** `用法: login <用户名> <密码>`

> ⚠️ **Security**: Never display the password in the response. Never distinguish "wrong username" vs "wrong password" — use generic "Invalid credentials".

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: login <user> <pwd> | Missing arguments |
| Invalid credentials | Wrong username or password |
| Account locked | Too many failed attempts |
| Account disabled | User deactivated |
| Connection failed | Server unreachable |

See [references/](references/) for related skills and API docs.
