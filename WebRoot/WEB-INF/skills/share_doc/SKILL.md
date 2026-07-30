---
name: share_doc
description: List or create document sharing links
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - read:document
tags: [document, share, link, sharing, public]
---

# Share Document

List existing share links or create a new share link for a document.

## Triggers

Share doc, create share, share link, 分享文档, 分享链接, 文档分享, 生成分享链接, make shareable, public link, share file, 创建分享链接, 文件分享, 分享这个文档

## Workflow

### Mode 1: List existing share links
1. **Extract vid + docId**: Parse from user input.
2. **Resolve IDs if missing**: If vid not provided → call `docsys repos list` to find doc's repository. If docId not provided → ask user or call `docsys docs list <vid>` to find the document.
3. **Call CLI**: `docsys share-list <vid> <docId>`
4. **Format output**: Display links with type, creation date, and expiry.

### Mode 2: Create new share link
1. **Extract parameters**: vid, docId, type, password.
2. **Resolve IDs if missing** (same as above).
3. **⚠️ Confirm before creating**: Show the document name and share type → ask user "Create this share link?" (confirm). If user declines, stop.
4. **Prompt for password** if type=1 but password not provided → ask user directly. Do not expose password in plaintext in output.
5. **Call CLI**: `docsys share-create <vid> <docId> [type] [password]`
6. **Present link securely**: Show the URL, note the access type, and warn if public.

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID |
| type | number | no | Share type: 0=public (default), 1=password, 2=private |
| password | string | no | Password for type=1 shares |

## Examples

### Example 1 — List links
User: "list share links for document 123"
Skill triggers (list mode) → resolves vid+docId → confirms with user → calls: `docsys share-list 1 123`
Output: Existing share links with type, date, expiry

### Example 2 — Public share
User: "create a public share link for document 456"
Skill triggers (create mode) → confirms "Create public share link for [doc name]?" → calls: `docsys share-create 1 456`
Output: New share URL with warning if public

### Example 3 — Password-protected share with full resolution
User: "生成带密码的分享链接"
Skill triggers (create mode, vid+docId unknown)
→ Step 1: `docsys repos list` → find doc's repository → resolve vid=3
→ Step 2: `docsys docs list 3` → find doc named "项目报告.pdf" → resolve docId=456
→ Step 3: ⚠️ Confirm "Create password-protected share for '项目报告.pdf'?" → user confirms
→ Step 4: Prompt user for password (read from terminal, do not log plaintext)
→ Step 5: `docsys share-create 3 456 1 <password>`
→ Output: Share URL, access type: password-protected, expiry (if set), ⚠️ warn if public

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| url | string | Full share URL |
| type | string | public / password-protected / private |
| created | string | Creation date |
| expires | string | Expiry date (or "never") |
| status | string | active / expired / revoked |

**Success --list (EN):** `Share links for doc {docId}:\n🔗 {url} | {type} | Created: {date} | Expires: {expires} | Status: {status}`
**Success --list (中文):** `文档 {docId} 的分享链接:\n🔗 {url} | 类型: {公开/密码保护/私有} | 创建: {date} | 过期: {expires} | 状态: {有效/已过期}`
**Success --create (EN):** `✅ Share link created: {url} | Type: {type} | Expires: {expires}`
**Success --create (中文):** `✅ 分享链接已创建：{url} | 类型: {type} | 过期: {expires}`
**Empty list (EN):** `No share links for doc {docId}. Create one with create-share.`
**Empty list (中文):** `文档 {docId} 没有分享链接。`
**⚠️ Public warning (EN):** `⚠️ This link is publicly accessible without authentication.`
**⚠️ Public warning (中文):** `⚠️ 此链接可公开访问，无需认证。`
**Error (EN):** `❌ {error_message}`
**Error (中文):** `❌ 分享失败：{错误信息}`

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: share list/create <vid> <docId> | Missing arguments |
| Document not found | Invalid vid or docId |
| Permission denied | No access |
| Share creation failed | API error |

## Backend Notes (DocSys v2.02.80)

- **`createDocShare.do` endpoint returns 404** — share creation is not available in v2.02.80
- `share-list` works (returns empty list — no shares exist)
- Skill should note this limitation when user requests share creation

See [references/](references/) for related skills and API docs.
