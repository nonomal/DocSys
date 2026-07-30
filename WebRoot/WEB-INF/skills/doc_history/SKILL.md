---
name: doc_history
description: Get version history of a document
category: document
version: 1.2.0
author: DocSys Team
permissions:
  - read:document
tags: [document, history, version, revision]
---

# Document History

Retrieve version history of a document showing all past versions.

## Triggers

History, document history, version history, 版本历史, 文档历史, 历史记录, 查看版本, 查看历史, past versions, file history, revision history, 文档版本, 版本记录, earlier versions, 修改历史, 文档变更, 何时修改

## Workflow

1. **Extract parameters**: Parse `vid` and `docId` from user input. Both are required.
2. **Resolve IDs if missing**: If vid missing → call `docsys repos list` to find available repos. If docId missing but user references "this document" → ask user to specify docId or navigate to the document context. **Do not guess or default to vid=1.**
3. **⚠️ Confirm inferred IDs**: If either ID was resolved from context (not explicitly provided) → show resolved name/ID and ask user to confirm before proceeding.
4. **Call CLI**: `docsys doc history <vid> <docId>`
5. **Format result**: Chronological list, newest first. Show: version number, date, author, change description.
6. **Zero versions**: If document has no history → return "No version history found" (normal state).

## CLI Command

```bash
docsys doc history <vid> <docId>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID |

## Examples

### Example 1
User: "show version history of document 123 in repo 1"
Skill triggers → Step 1: vid=1, docId=123 → Step 3: IDs explicit, no confirm needed → calls: `docsys doc history 1 123`
Output: List of versions with dates and authors

### Example 2
User: "查看文档 456 的版本历史"
Skill triggers → Step 1: docId=456, vid=missing → Step 2: calls `docsys repos list` to find available repos → user specifies vid=3 → Step 3: confirm docId=456, vid=3 → calls: `docsys doc history 3 456`
Output: 版本列表（含时间、作者、变更描述）

### Example 3
User: "what are the past versions of this file"
Skill triggers → Step 1: both vid and docId missing → Step 2: calls `docsys repos list` → user specifies vid=2 → calls `docsys docs list 2` to find docId → user selects docId=789 → Step 3: confirm vid=2, docId=789 → calls: `docsys doc history 2 789`
Output: Version history

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| version | string | Version number or revision ID |
| date | string | Version creation date |
| author | string | User who made the change |
| description | string | Change summary or message |
| docId | number | Document ID |

**Success (EN):** `📋 Version history (newest first):\nv{version} | {date} | {author} | {description}\n...`
**Success (中文):** `📋 版本历史（最新优先）：\nv{version} | {date} | {author} | {description}\n...`
**No history:** `ℹ️ No version history found for doc {docId} (this is normal for new documents).`
Error: "Document not found" | "Permission denied"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: doc history <vid> <docId> | Missing vid or docId |
| Document not found | Invalid vid or docId |
| Permission denied | No read access |

See [references/](references/) for related skills and API docs.
