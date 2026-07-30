---
name: unlock_doc
description: Unlock a document to allow editing
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - write:document
tags: [document, unlock, release, editing]
---

# Unlock Document

Unlock a document to allow editing by other users.

## Triggers

Unlock doc, unlock document, unlock file, 解除锁定, 解锁文档, 解锁文件, release lock, unprotect, 文件解锁, 文档解锁, remove lock, remove protection, 解除文档锁定

## Workflow

1. **Extract parameters**: Parse vid and docId from user input.
2. **Missing parameters → prompt**: If vid or docId not provided → ask user to supply it. **Do not call CLI with missing arguments.**
3. **⚠️ Confirm before unlocking**: If lock ownership matters (another user locked it) → warn and confirm. Otherwise → confirm "Unlock document [docId]?"
4. **Call CLI**: `docsys unlock <vid> <docId>`
5. **Present result**: If "Not locked" returned → treat as success (already unlocked, exits cleanly). If other error → explain.

## CLI Command

```bash
docsys unlock <vid> <docId>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID |

## Examples

### Example 1
User: "unlock document 123 in repo 1"
Skill triggers → calls: `docsys unlock 1 123`
Output: Unlock confirmation

### Example 2
User: "解锁文档 456"
Skill triggers → Step 1: docId=456, vid=missing → Step 2: prompts user for vid → user says "repo 3" → calls: `docsys unlock 3 456`
Output: 解锁结果

### Example 3
User: "release the lock on this file"
Skill triggers → Step 1: both vid and docId missing → Step 2: prompts user for both → user specifies vid=4, docId=789 → Step 3: confirm "Unlock document 789 in repo 4?" → calls: `docsys unlock 4 789`
Output: Unlock result

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| docId | number | Document ID |
| vid | number | Repository ID |
| status | string | "unlocked" | "already_unlocked" |

**Success (EN):** `✅ Document {docId} in repo {vid} unlocked.`
**Success (中文):** `✅ 文档 {docId}（仓库 {vid}）已解锁。`
**Already unlocked:** `ℹ️ Document {docId} is not locked (already unlocked).`
Error: "Document not found" | "Permission denied" | "Cannot unlock — locked by another user"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: unlock <vid> <docId> | Missing arguments |
| Document not found | Invalid vid or docId |
| Not locked | Already unlocked |
| Cannot unlock | Locked by another user |
| Permission denied | No write access |

## Backend Notes (DocSys v2.02.80)

- The `unlockDoc.do` API uses `reposId` as the parameter name (not `vid`)
- **Backend bug**: `unlockDoc.do` throws `NullPointerException` at `isForceUnlockAllow()` — the unlock operation is unavailable in v2.02.80. Skill should warn users of this limitation.
- If unlock is needed, contact the lock holder directly or restart the DocSys service to clear lock state.

See [references/](references/) for related skills and API docs.
