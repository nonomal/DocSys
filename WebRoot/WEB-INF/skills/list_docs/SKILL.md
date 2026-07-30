---
name: list_docs
description: List documents and folders in a repository or folder
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - read:document
tags: [document, list, folder, browse]
---

# List Documents

List all documents and folders in a repository or specific folder.

## Triggers

List docs, list documents, list files, list folder, 列出文档, 查看文档, 文件列表, 列出文件, 查看文件夹内容, ls, what's in this folder, show me the files, browse repository, 浏览文件夹, 查看有哪些文件, 查看仓库内容

## Workflow

1. **Extract parameters**: Parse vid and pid from user input.
2. **⚠️ Confirm vid before listing**: If vid is missing or ambiguous → ask user. If user gives repo name → call `docsys repos list` to resolve name → vid. Present resolved vid to user for confirmation: `"Listing documents in repository [name] (vid={resolved_vid}). Continue?"`
3. **Call CLI**: `docsys doc list <vid> [pid]` (pid defaults to 0 = root)
4. **Pagination**: If >100 items returned → group into folders/files, show top 50 with note "Showing 50 of N items. Use pid filter to narrow."
5. **Format output**: Folders listed first, then files. Per item: name, size, modified date.

## CLI Command

```bash
docsys doc list <vid> [pid]
docsys ls <vid> [pid]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| pid | number | no | Parent folder ID (default: 0 = root) |

## Examples

### Example 1
User: "list documents in repository 1"
Skill triggers → calls: `docsys doc list 1`
Output: Folders and files at root level of repo 1

### Example 2
User: "列出文件夹 5 中的内容，仓库是 1"
Skill triggers → calls: `docsys doc list 1 5`
Output: 列出仓库 1 中文件夹 5 的内容

### Example 3
User: "ls in this repo"
Skill triggers → Step 1: vid=missing → Step 2: calls `docsys repos list` → user specifies vid=1 → Step 3: calls: `docsys ls 1`
Output: Folder contents at root

### Example 4 (pagination)
User: "list all files in repo 2"
Skill triggers → Step 1: vid=2 → Step 3: calls: `docsys doc list 2` → Step 4: returns 150 items (>100) → shows first 50 items with note "Showing 50 of 150 items. Use pid filter or browse subfolders."

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| name | string | File or folder name |
| type | string | `folder` or `file` |
| size | string | File size (empty for folders) |
| modified | string | Last modified date |
| pid | number | Parent folder ID |

**Success (EN):** `📁 {name}/ | {size} | {modified}\n📄 {name} | {size} | {modified}`
**Success (中文):** `📁 {name}/ | {size} | {modified}\n📄 {name} | {size} | {modified}`
**Paginated (EN):** `📁 .../\n📄 ... | ...\n⚠️ Showing 50 of {N} items. Use subfolder PIDs to narrow results.`
**Paginated (中文):** `📁 .../\n📄 ... | ...\n⚠️ 显示 {N} 个项目中的前 50 个。使用子文件夹 PID 缩小范围。`
**Not found (EN):** `❌ Repository {vid} not found. Check available repos with 'docsys repos list'.`
**Not found (中文):** `❌ 仓库 {vid} 不存在。使用 'docsys repos list' 查看可用仓库。`
**Permission denied (EN):** `❌ Permission denied. You don't have access to this repository.`
**Permission denied (中文):** `❌ 权限不足。您没有此仓库的访问权限。`
Error: "Repository not found" | "Folder not found" | "Permission denied"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: doc list <vid> [pid] | Missing vid |
| Repository not found | Invalid vid |
| Folder not found | Invalid pid |
| Permission denied | No read access |

## Backend Notes (DocSys v2.02.80)

- The endpoint `/Repos/getDocList.do` does not exist in v2.02.80 (returns 404)
- **Workaround**: Use `docsys search <vid> "."` — `searchDoc.do` with `searchWord=.` returns all documents in the repository as a fallback enumeration method
- The search result includes `pid`, `name`, `docId`, and `type` for each item

See [references/](references/) for related skills and API docs.
