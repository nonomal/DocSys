---
name: list_repos
description: List all accessible repositories in DocSystem
category: repository
version: 1.1.0
author: DocSys Team
permissions:
  - read:repository
tags: [repository, list, repos]
---

# List Repositories

List all repositories the current user has access to. Shows name, ID, path, type, and document count.

## Triggers

List repos, list all repos, show repos, list my repos, what repos do I have, what repositories exist, 列出仓库, 查看仓库, 仓库列表, 有哪些仓库, 我的仓库, 查看仓库列表, 列出所有仓库, 查看有哪些仓库, repositories, all repos, available repos, repo list, show repositories

## Workflow

1. **No parameters required** — call CLI directly.
2. **Call CLI**: `docsys repos list`
3. **Format output**: Show per repository: name, VID, path, type (Local | SVN | GIT), document count.
4. **Large list handling**: If >50 repos returned → group by type or show top 20 with "Showing X of Y repositories" note.
5. **Empty state**: If no repositories found → return "No repositories found" (this is a normal state, not an error).

## CLI Command

```bash
docsys repos list
```

## Parameters

None required.

## Examples

### Example 1
User: "列出所有仓库"
Skill triggers → calls: `docsys repos list`
Output: Lists all accessible repositories with name, VID, path, type (Local | SVN | GIT), document count

### Example 2
User: "what repositories do I have access to?"
Skill triggers → calls: `docsys repos list`
Output: Lists repositories with name, VID, type, document count

### Example 3
User: "show all repos"
Skill triggers → calls: `docsys repos list`
Output: Repository list with VID, path, type (Local/SVN/GIT), document count

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| name | string | Repository name |
| VID | number | Repository ID |
| path | string | Repository path |
| type | string | Type: Local / SVN / GIT |
| doc_count | string | Number of documents |

**Success (EN):** `📦 {name} | VID: {id} | Type: {type} | Path: {path} | Docs: {count}`
**Success (中文):** `📦 {name} | 仓库ID: {id} | 类型: {type} | 路径: {path} | 文档数: {count}`
**Paginated (EN):** `Showing {X} of {Y} repositories. Use filters to narrow results.`
**Paginated (中文):** `显示 {X}/{Y} 个仓库。可使用过滤器缩小范围。`
**Empty (EN):** `No repositories found. You may not have access to any repositories yet.`
**Empty (中文):** `未找到仓库。您当前可能没有任何仓库的访问权限。`
**Multi-repo table (EN):** `Showing {Y} repositories:\n📦 {name} | VID: {id} | {type} | {count} docs\n...`
**Multi-repo table (中文):** `共 {Y} 个仓库:\n📦 {name} | VID: {id} | {类型} | {文档数} 个文档\n...`
**Connection failed (EN):** `❌ Cannot connect to DocSystem server. Check network and server status.`
**Connection failed (中文):** `❌ 无法连接到 DocSystem 服务器。请检查网络连接和服务器状态。`

## Error Handling

| Error | Cause |
|-------|-------|
| No repositories found | User has no assigned repositories |
| Connection failed | DocSystem server unreachable |
| Permission denied | User lacks repository read access |

See [references/](references/) for related skills and API docs.
