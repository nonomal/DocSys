---
name: add_doc
description: Create a new folder in a repository
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - write:document
tags: [document, folder, create, mkdir, add]
---

# Add Document/Folder

Create a new folder in a repository.

## Triggers

Add doc, new folder, create folder, mkdir, 新建文件夹, 新建文档, 新建目录, 创建文件夹, make folder, create directory, 创建文件夹, 新建一个文件夹

## Workflow

1. **Extract parameters**: Parse vid and name from user input.
2. **Resolve vid if missing**: If vid not provided → call `docsys repos list` to resolve name to vid. If still missing → ask user.
3. **Validate name**: Check for forbidden chars (`/\:*?"<>|`) and max 255 chars. If invalid → reject with guidance.
4. **⚠️ Confirm before creating**: Show repo, folder name → ask "Create folder [name]?" (confirm). If user declines → stop.
5. **Call CLI**: `docsys doc add <vid> <name>` or `docsys mkdir <vid> <name>`
6. **Present result**: Show new folder ID and location.

## CLI Command

```bash
docsys doc add <vid> <name>
docsys mkdir <vid> <name>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| name | string | yes | Folder name (no `/\:*?"<>|` chars, max 255) |

## Examples

### Example 1
User: "create a folder called reports in repo 1"
Skill triggers → calls: `docsys doc add 1 reports`
Output: Folder created with new folder ID

### Example 2
User: "新建文件夹 Q4报告 在仓库 1 中"
Skill triggers → calls: `docsys mkdir 1 Q4报告`
Output: 文件夹创建成功，返回 folder ID

### Example 3
User: "mkdir myproject"
Skill triggers → Step 1: name="myproject", vid=missing → Step 2: calls `docsys repos list` to find available repos → user specifies vid=1 → Step 4: confirm "Create folder 'myproject' in repo 1?" → calls: `docsys mkdir 1 myproject`
Output: Folder created with new folder ID

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| name | string | Folder name created |
| folder_id | number | New folder ID |
| vid | number | Repository ID |
| parent_id | number | Parent folder ID (0 = root) |

**Success (EN):** `✅ Folder created | {name} (ID: {folder_id}) in repo {vid}`
**Success (中文):** `✅ 文件夹已创建 | {name}（ID: {folder_id}）位于仓库 {vid}`
Error: "Folder already exists" | "Invalid name — forbidden chars: /\\:*?\"<>|" | "Usage: doc add <vid> <name>"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: doc add <vid> <name> | Missing arguments |
| Folder name required | Empty name |
| Invalid characters | Forbidden chars in name |
| Folder already exists | Name conflict |
| Permission denied | No write access |
| Repository not found | Invalid vid |

See [references/](references/) for related skills and API docs.
