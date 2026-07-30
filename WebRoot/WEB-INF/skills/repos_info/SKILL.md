---
name: repos_info
description: Get detailed information about a specific repository
category: repository
version: 1.2.0
author: DocSys Team
permissions:
  - read:repository
tags: [repository, info, details, get]
---

# Repository Info

Get detailed information about a specific repository.

## Triggers

Repo info, repos info, repository info, 仓库详情, 仓库信息, 查看仓库详情, repo details, get repo, 仓库信息, what is repo 1, describe repo, 仓库基本信息, repo status, 仓库状态, repo 大小, repo type, 仓库类型, how many docs in repo

## Workflow

1. **Extract vid**: Parse `vid` from user input if provided.
2. **⚠️ Confirm scope if vid missing**: If user did not specify a vid → ask "Show details for all repositories, or a specific one?" before calling any CLI. Do not auto-list all without confirmation.
3. **Resolve name→vid if needed**: If user gave a repository name (not ID) → call `docsys repos list` to find matching vid, confirm with user.
4. **Call CLI**: `docsys repos get <vid>`
5. **Present result**: Name, VID, type (Local | SVN | GIT), storage path, document/folder count, dates.
6. **Large output handling**: If listing all repos → show top 10 with "and N more repositories" summary.

## CLI Command

```bash
docsys repos get <vid>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |

## Examples

### Example 1
User: "show details for repository 1"
Skill triggers → Step 1: vid=1 → Step 3: explicit, no confirm needed → calls: `docsys repos get 1`
Output: Repository name, ID, type, path, document count, last modified

### Example 2
User: "仓库 2 的详细信息"
Skill triggers → Step 1: vid=2 → calls: `docsys repos get 2`
Output: 仓库名称、ID、类型、路径、文档数等

### Example 3
User: "what repos do I have"
Skill triggers → Step 1: vid missing → Step 2: asks user "Show details for all, or a specific one?" → user says "all" → Step 4: calls `docsys repos get` for each, Step 6: shows top 10 with summary
Output: Repository details for accessible repos

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| name | string | Repository name |
| vid | number | Repository ID |
| type | string | Local / SVN / GIT |
| path | string | Storage path |
| doc_count | number | Number of documents |
| folder_count | number | Number of folders |
| created | string | Creation date |
| updated | string | Last modified date |

**Success (EN):** `✅ Repository: {name} (vid={vid}) | Type: {type} | Path: {path} | Docs: {doc_count} | Updated: {updated}`
**Success (中文):** `✅ 仓库：{name}（vid={vid}）| 类型：{type} | 路径：{path} | 文档：{doc_count} | 更新：{updated}`
**Error:** "Repository not found" | "Permission denied"

## Error Handling

| Error | Cause |
|-------|-------|
| Repository ID is required | Missing vid |
| Repository not found | Invalid vid |
| Permission denied | No read permission |

See [references/](references/) for related skills and API docs.
