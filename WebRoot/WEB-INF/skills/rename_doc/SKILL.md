---
name: rename_doc
description: Rename a document or folder within a repository
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - write:document
tags: [document, rename, mv, move-rename]
---

# Rename Document

Rename a document or folder within a repository.

## Triggers

Rename doc, rename document, rename file, 重命名文档, 文件重命名, 文档重命名, rename this, change name, 改文件名, rename the file, rename folder, mv doc

## Workflow

1. **Extract parameters**: Parse vid, docId, newName from user input.
2. **Resolve IDs if missing**: If vid not provided → call `docsys repos list` to find available repos and ask user to confirm. If docId not provided → call `docsys docs list <vid>` to locate the document.
3. **⚠️ Confirm before renaming**: Show old name, new name, and repository → ask "Rename to [newName]?" (confirm). If user declines → stop.
4. **Call CLI**: `docsys doc rename <vid> <docId> <newName>`
5. **Present result**: Confirm old→new name. If error → explain and note rollback (rename back to original).
6. **Rollback**: If rename failed → call `docsys doc rename <vid> <docId> <originalName>`

## CLI Command

```bash
docsys doc rename <vid> <docId> <newName>
docsys mv <vid> <docId> <newName>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID to rename |
| newName | string | yes | New name (no `/\:*?"<>|`, max 255 chars) |

## Examples

### Example 1
User: "rename document 123 to new_report.pdf"
Skill triggers → calls: `docsys doc rename 1 123 new_report.pdf`
Output: Rename result

### Example 2
User: "把文档 456 重命名为 2025报告.pdf"
Skill triggers → calls: `docsys doc rename 1 456 2025报告.pdf`
Output: 重命名结果

### Example 3
User: "mv doc 789 to updated_data.xlsx"
Skill triggers → calls: `docsys mv 1 789 updated_data.xlsx`
Output: Rename result

## Output Format

Success: Brief confirmation showing old and new name.
Error: "Document not found", "Invalid name", "Name conflict"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: doc rename <vid> <docId> <newName> | Missing arguments |
| Document not found | Invalid vid or docId |
| Invalid characters | Forbidden chars in name |
| Name conflict | Name already exists in folder |
| Permission denied | No write access |

See [references/](references/) for related skills and API docs.
