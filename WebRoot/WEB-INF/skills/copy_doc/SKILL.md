---
name: copy_doc
description: Copy a document to another folder within the same repository
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - write:document
tags: [document, copy, duplicate, cp]
---

# Copy Document

Copy a document to another folder within the same repository. Auto-renames if name conflicts.

## Triggers

Copy doc, copy document, copy file, 复制文档, 复制文件, duplicate doc, make a copy, clone doc, 文件复制, 文档复制, cp file, 复制这个文件

## Workflow

1. **Extract parameters**: Parse vid, docId, targetPid from user input.
2. **Resolve IDs if name given**: If vid missing → call `docsys repos list` to resolve name to vid. If target is a folder name (e.g., "backup") → call `docsys docs list <vid>` to find folder IDs by name.
3. **⚠️ Confirm before copying**: Show source doc name and target folder → warn if auto-rename will occur due to name conflict. Ask "Copy [doc] to [folder]?" (confirm). If user declines → stop.
4. **Call CLI**: `docsys doc copy <vid> <docId> <targetPid>`
5. **Present result**: Show new document ID and location. Note if auto-renamed due to conflict.

## CLI Command

```bash
docsys doc copy <vid> <docId> <targetPid>
docsys cp <vid> <docId> <targetPid>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID to copy |
| targetPid | number | yes | Target folder ID (0 = root) |

## Examples

### Example 1
User: "copy document 123 to folder 5 in repo 1"
Skill triggers (workflow) → resolves IDs → confirms "Copy doc 123 to folder 5?" → calls: `docsys doc copy 1 123 5`
Output: Copy result with new document ID

### Example 2
User: "复制文档 456 到根目录"
Skill triggers → resolves vid → confirms → calls: `docsys doc copy <vid> 456 0`
Output: 复制结果，新文档 ID

### Example 3
User: "cp doc 789 to folder backup"
Skill triggers → resolves "backup" folder name to targetPid via `docsys docs list <vid>` → confirms → calls: `docsys cp 1 789 <targetPid>`
Output: Copy result

## Output Format

Success: Brief confirmation with new document ID. Auto-renames if conflict.
Error: "Document not found", "Target folder not found", "Permission denied"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: doc copy <vid> <docId> <targetPid> | Missing arguments |
| Document not found | Invalid vid or docId |
| Target folder not found | Invalid targetPid |
| Insufficient storage | Not enough space |
| Permission denied | No write access |

See [references/](references/) for related skills and API docs.
