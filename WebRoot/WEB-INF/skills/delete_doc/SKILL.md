---
name: delete_doc
description: Delete a document or folder from a repository
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - write:document
tags: [document, delete, remove, rm, destructive]
---

# Delete Document

Permanently delete a document or folder from a repository.

## Triggers

Delete doc, delete document, delete file, rm, remove doc, 删除文档, 删除文件, 永久删除, 移除文档, 删掉这个文件, erase document, remove file, 文档删除

## Workflow

1. **Extract parameters**: Parse vid and docId from user input.
2. **Resolve IDs if missing**: If vid not provided → call `docsys repos list` to resolve. If docId not provided → call `docsys docs list <vid>` to locate document by name.
3. **⚠️⚠️ MANDATORY CONFIRM — DESTRUCTIVE**: Show document name, repository, and docId → ask user to type "delete" to confirm. **This is permanent and cannot be undone.** If user declines → stop.
4. **Call CLI**: `docsys doc delete <vid> <docId>`
5. **Present result**: Confirm deletion is complete. No undo available.

## CLI Command

```bash
docsys doc delete <vid> <docId>
docsys rm <vid> <docId>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID to delete |

## Examples

### Example 1
User: "delete document 789 from repo 1"
Skill triggers → calls: `docsys doc delete 1 789`
Output: Deletion result

### Example 2
User: "删除仓库 2 中的文档 456"
Skill triggers → calls: `docsys doc delete 2 456`
Output: 删除结果

### Example 3
User: "rm doc 123"
Skill triggers → Step 1: docId=123, vid=missing → Step 2: calls `docsys repos list` to resolve vid → user specifies vid=2 → Step 3: confirm "Type 'delete' to permanently remove document 123 from repo 2" → calls: `docsys rm 2 123`
Output: Deletion result

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| docId | number | Document ID deleted |
| vid | number | Repository ID |
| name | string | Document name |
| type | string | "document" or "folder" |

**Success (EN):** `✅ Document '{name}' (docId={docId}) permanently deleted from repo {vid}. No undo available.`
**Success (中文):** `✅ 文档 '{name}'（docId={docId}）已从仓库 {vid} 永久删除。无法撤销。`
Error: "Document not found" | "Permission denied" | "Cannot delete — system document"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: doc delete <vid> <docId> | Missing arguments |
| Document not found | Invalid vid or docId |
| Cannot delete system document | Protected document |
| Permission denied | No write access |
| Delete failed | API error |

See [references/](references/) for related skills and API docs.
