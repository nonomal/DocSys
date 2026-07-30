---
name: lock_doc
description: Lock a document to prevent editing by others
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - write:document
tags: [document, lock, protect, editing]
---

# Lock Document

Lock a document to prevent concurrent editing by other users.

## Triggers

Lock doc, lock document, lock file, 锁定文档, 锁定文件, protect doc, 文档锁定, 文件锁定, lock for editing, prevent edits, 加锁

## Workflow

1. **Extract parameters**: Parse vid, docId, and lock type (1=exclusive, 2=shared) from user input.
2. **Resolve IDs if missing**: If vid missing → call `docsys repos list`. If docId missing → call `docsys docs list <vid>`.
3. **⚠️ Confirm before locking**: Show document, repository, and lock type → warn "This will prevent other users from editing until unlocked." Ask "Lock [docId]?" (confirm). If user declines → stop.
4. **Call CLI**: `docsys lock <vid> <docId> [type]`
5. **Present result**: Confirm lock acquired with type. If "Already locked" → show lock holder name if returned, and suggest: contact the holder directly, or use `docsys lock <vid> <docId> 2` (shared) to take a shared lock instead.

## CLI Command

```bash
docsys lock <vid> <docId> [type]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID |
| type | number | no | Lock type: 1=exclusive (default), 2=shared |

## Examples

### Example 1
User: "lock document 123 in repo 1"
Skill triggers → calls: `docsys lock 1 123`
Output: Lock confirmation

### Example 2
User: "锁定文档 456 in repo 2"
Skill triggers → Step 1: vid=2, docId=456 → Step 3: confirm → calls: `docsys lock 2 456`
Output: 锁定结果

### Example 3
User: "lock doc 789 exclusively in repo 3"
Skill triggers → Step 1: vid=3, docId=789, type=1 (exclusive) → Step 3: confirm "Exclusive lock prevents ALL other users" → calls: `docsys lock 3 789 1`
Output: Lock result

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| docId | number | Document ID |
| vid | number | Repository ID |
| lockType | number | Lock type: 1=exclusive, 2=shared |
| locked_by | string | Username of lock holder |

**Success (EN):** `🔒 Document {docId} in repo {vid} locked ({type: exclusive/shared}). Locked by: {username}`
**Success (中文):** `🔒 文档 {docId}（仓库 {vid}）已锁定（类型: {独占/共享}）。锁定者: {username}`
**Already locked (EN):** `🔒 Document {docId} is already locked by {username}. Contact them or use shared lock (type 2).`
**Already locked (中文):** `🔒 文档 {docId} 已被 {username} 锁定。请联系对方或使用共享锁（type 2）。`
**Error (EN):** `❌ Lock failed: {reason}`
**Error (中文):** `❌ 锁定失败：{原因}`

## Backend Notes (DocSys v2.02.80)

- The `lockDoc.do` API uses `reposId` as the parameter name (not `vid`)
- CLI correctly sends `reposId={vid}` — ensure any direct API calls also use `reposId`
- **Backend limitation**: `lockDoc.do` always returns `docId=0` in the response regardless of input — the lock operation succeeds but the response does not reflect the locked document's ID

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: lock <vid> <docId> | Missing arguments |
| Document not found | Invalid vid or docId |
| Already locked | Locked by another user |
| Permission denied | No write access |

See [references/](references/) for related skills and API docs.
