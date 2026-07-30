---
name: create_repos
description: Create a new repository in DocSystem
category: repository
version: 1.1.0
author: DocSys Team
permissions:
  - write:repository
tags: [repository, create, add, new]
---

# Create Repository

Create a new document repository in DocSystem.

## Triggers

Create repo, create repository, new repo, add repo, add repository, 创建仓库, 新建仓库, 添加仓库, 建立仓库, 创建一个仓库, make a repo, register repository

## Workflow

1. **Parse parameters**: Extract `name` and `path` from user input.
2. **Validate name**: Check for forbidden characters (`/\:*?"<>|`). If invalid → reject with guidance.
3. **Validate path**: If path does not exist → warn user. The path should be created or confirmed writable before proceeding.
4. **⚠️ Confirm before creating**: Show name and path → ask "Type [name] to confirm creating repository at [path]." This action is irreversible. If user declines or does not type the name → stop.
5. **Call CLI**: `docsys repos add <name> <path>`
6. **Present result**: Show new repository VID and confirm creation.

## CLI Command

```bash
docsys repos add <name> <path>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| name | string | yes | Repository name (no `/ \ : * ? " < > |`) |
| path | string | yes | Storage path on disk |

## Examples

### Example 1
User: "create a new repo called MyProject at F:/data/myrepo"
Skill triggers → calls: `docsys repos add MyProject F:/data/myrepo`
Output: Success confirmation with new repository VID

### Example 2
User: "新建仓库 项目A 路径 E:/projects/A"
Skill triggers → calls: `docsys repos add 项目A E:/projects/A`
Output: 创建成功，返回新仓库 VID

### Example 3
User: "add a repository named Reports at /mnt/storage/reports"
Skill triggers → calls: `docsys repos add Reports /mnt/storage/reports`
Output: Repository created successfully

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| name | string | Repository name |
| VID | number | New repository ID |
| path | string | Storage path |
| type | string | Repository type (Local/SVN/GIT) |

**Success (EN):** `✅ Repository created: {name} (VID: {id}) | Type: {type} | Path: {path}`
**Success (中文):** `✅ 仓库创建成功：{name}（仓库ID: {id}）| 类型: {type} | 路径: {path}`
**Already exists (EN):** `❌ Repository named "{name}" already exists (VID: {id}). Choose a different name.`
**Already exists (中文):** `❌ 仓库"{name}"已存在（VID: {id}）。请使用其他名称。`

## Error Handling

| Error | Cause |
|-------|-------|
| Repository name required | Empty name provided |
| Storage path required | Empty path provided |
| Invalid characters | Name contains forbidden chars |
| Repository already exists | Duplicate name |
| Access denied | No write permission |

See [references/](references/) for related skills and API docs.
