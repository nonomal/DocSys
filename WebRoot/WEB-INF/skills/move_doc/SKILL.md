---
name: move_doc
description: Move a document to another folder within the same repository
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - write:document
tags: [document, move, relocate, folder]
---

# Move Document

Move a document or folder to another location within the same repository.

## Triggers

Move doc, move document, move file, 移动文档, 移动文件, 移动到, relocate doc, transfer doc, move to folder, 移动这个文件, 文件移动, mv

## Workflow

1. **Extract parameters**: Parse vid, docId, targetPid from user input.
2. **Resolve IDs if name given**: If target is a folder name (e.g., "Archive") instead of numeric ID → call `docsys docs list <vid>` to find folder names and map to targetPid. If vid missing → call `docsys repos list` to resolve.
3. **⚠️ Confirm before moving**: Show source location, target location, and document name → ask "Move [doc name] to [target]?" (confirm). If user declines → stop.
4. **Call CLI**: `docsys doc move <vid> <docId> <targetPid>`
5. **Present result**: Confirm from/to locations. If error → explain and suggest rollback (reverse the move).
6. **Rollback**: If move failed → to undo: call `docsys doc move <vid> <docId> <originalPid>`

## CLI Command

```bash
docsys doc move <vid> <docId> <targetPid>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID to move |
| targetPid | number | yes | Target folder ID (0 = root) |

## Examples

### Example 1
User: "move document 123 to folder 5 in repo 1"
Skill triggers → calls: `docsys doc move 1 123 5`
Output: Move result

### Example 2
User: "把文档 456 移动到根目录，仓库是 2"
Skill triggers → calls: `docsys doc move 2 456 0`
Output: 移动到根目录结果

### Example 3
User: "move file 789 to folder Archive"
Skill triggers → Step 1: docId=789, targetPid=missing → Step 2: calls `docsys docs list <vid>` to find folder named "Archive" → resolve targetPid=6 → Step 3: confirm → calls: `docsys doc move <vid> 789 6`
Output: Move result

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| docId | number | Document ID moved |
| vid | number | Repository ID |
| from_pid | number | Original folder ID |
| to_pid | number | Target folder ID |

**Success (EN):** `✅ Document {docId} moved | repo {vid} | from folder {from_pid} → {to_pid}`
**Success (中文):** `✅ 文档 {docId} 已移动 | 仓库 {vid} | 从 {from_pid} → {to_pid}`
**Rollback:** `To undo: docsys doc move {vid} {docId} {from_pid}`
Error: "Document not found" | "Target folder not found" | "Permission denied"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: doc move <vid> <docId> <targetPid> | Missing arguments |
| Document not found | Invalid vid or docId |
| Target folder not found | Invalid targetPid |
| Already in this folder | source == target |
| Permission denied | No write access |

See [references/](references/) for related skills and API docs.
