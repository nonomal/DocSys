---
name: search_in_repo
description: Full-text search within a specific repository
category: search
version: 1.1.0
author: DocSys Team
permissions:
  - read:document
tags: [search, find, query, repo-specific]
---

# Search In Repository

Full-text search within a specific repository.

## Triggers

Search in repo, search within repository, 在仓库中搜索, repo search, 仓库内搜索, search inside, search this repo, repository search, 仓库搜索, search repo, find in repo, 在这个仓库里找, 在这个仓库里找, search project repo, search my docs

## Workflow

1. **Extract parameters**: Parse `query` (search keywords) and `vid` (repository ID) from user input.
2. **Resolve vid if name given**: If user references repository by name (e.g., "project repo", "项目仓库") instead of numeric ID → call `docsys repos list` to find matching name, map to vid. If ambiguous, ask user which repo.
3. **Call CLI**: `docsys search <query> <vid>`
4. **Format results**: Return list of matching documents with filename, path, and relevance indicator.
5. **Zero-results handling**: If no results, suggest broadening query or removing filters.

## CLI Command

```bash
docsys search <query> <vid>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| query | string | yes | Search keywords |
| vid | number | yes | Repository ID to search within |

## Examples

### Example 1
User: "search for reports in repository 1"
Skill triggers → calls: `docsys search reports 1`
Output: Documents matching "reports" in repo 1

### Example 2
User: "在仓库 2 中搜索机器学习相关文档"
Skill triggers → calls: `docsys search 机器学习 2`
Output: 搜索结果

### Example 3 — Natural-language vid resolution
User: "search project repo for architecture files"
Skill triggers → Step 1: "project repo" is a name, not a numeric vid
Step 2: call `docsys repos list` → find repository named "project repo" → resolve vid=5
Step 3: call `docsys search architecture 5`
Output: Documents matching "architecture" in repo "project repo"

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| filename | string | Name of the matched document |
| path | string | Full path within the repository |
| size | string | File size |
| date | string | Last modified date |
| relevance | string | Match relevance: high / medium / low |

**Success (EN):** `Found {N} results in repo {vid}:\n📄 {filename} | {path} | {size} | {date} | relevance: {high/medium/low}`
**Success (中文):** `在仓库 {vid} 中找到 {N} 条结果:\n📄 {filename} | {path} | {size} | {date} | 相关度: {高/中/低}`
**Zero results (EN):** `No documents found matching "{query}" in repo {vid}. Try broader keywords or check spelling.`
**Zero results (中文):** `在仓库 {vid} 中未找到包含"{query}"的文档。请尝试更宽泛的关键词。`
**Repo not found (EN):** `❌ Repository {vid} not found. Run 'docsys repos list' to find available repositories.`
**Repo not found (中文):** `❌ 仓库 {vid} 不存在。请运行 'docsys repos list' 查看可用仓库。`
**Permission denied (EN):** `❌ Permission denied. You don't have access to repository {vid}.`
**Permission denied (中文):** `❌ 权限不足。您没有仓库 {vid} 的访问权限。`
**Connection failed (EN):** `❌ Cannot connect to DocSystem server. Check network and try again.`
**Connection failed (中文):** `❌ 无法连接到 DocSystem 服务器。请检查网络连接。`

## Error Handling

| Error | Cause |
|-------|-------|
| Query is required | Empty search query |
| Repository not found | Invalid vid |
| No results found | Try broader keywords |

See [references/](references/) for related skills and API docs.
