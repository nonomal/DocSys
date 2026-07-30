---
name: search_doc
description: Full-text search across all repositories
category: search
version: 1.1.0
author: DocSys Team
permissions:
  - read:document
tags: [search, find, query, full-text]
---

# Search Documents

Full-text search across all accessible repositories.

## Triggers

Search, search docs, 搜索, 查找, 检索, find documents, find files, search for, 搜索文档, 查找文件, 查询, search everything, global search, find, 全文搜索, 文件搜索, 文件查找

## Workflow

1. **Parse query**: Extract search keywords from user input. Multi-word queries: pass as space-separated arguments (e.g., `docsys search machine learning papers`). Phrase search: wrap in double quotes if supported (e.g., `docsys search "project timeline"`).
2. **Optional vid filter**: If user specifies a repository name → call `docsys repos list` to resolve name to vid; pass `vid` to CLI.
3. **Call CLI**: `docsys search <query> [vid]`
4. **Zero-results handling**: If no results → suggest broadening: remove filters, try fewer keywords, or search without a vid filter. Example: "No results for 'complex phrase'. Try 'complex' alone or without repo filter."
5. **Overflow handling**: If too many results → return top 20 sorted by relevance, note "showing 20 of N results". Suggest adding a vid filter or more specific terms.
6. **Format output**: Show per result: repository name, filename, path, size, date.

## CLI Command

```bash
docsys search <query> [vid]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| query | string | yes | Search keywords (space-separated for multi-word) |
| vid | number | no | Limit search to specific repository |

## Examples

### Example 1
User: "search for project documents"
Skill triggers → calls: `docsys search project`
Output: List of matching documents with repository, path, size, date

### Example 2
User: "搜索包含 Q4 报告的文档"
Skill triggers → calls: `docsys search Q4 报告`
Output: 匹配的文档列表

### Example 3
User: "find machine learning papers"
Skill triggers → Step 1: query="machine learning" → calls: `docsys search machine learning papers`
Output: Matching documents

### Example 4
User: "搜索 Claude AI 相关文档"
Skill triggers → Step 1: query="Claude AI" → calls: `docsys search Claude AI`
Output: 匹配文档列表

### Example 5 (zero results → refinement)
User: "搜索 项目计划 2024 完整版"
Skill triggers → Step 1: query="项目计划 2024 完整版" → Step 3: 0 results → Step 4: suggests "No results. Try '项目计划' alone or broaden query."

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| repo | string | Repository name |
| filename | string | Document filename |
| path | string | File path within repo |
| size | string | File size |
| date | string | Last modified date |
| relevance | string | Relevance score (if available) |

**Success (EN):** `{repo} | {filename} | {path} | {size} | {date}`
**Success (中文):** `{repo} | {filename} | {path} | {size} | {date}`
**Zero results:** `⚠️ No results for "{query}". Try broader terms or remove repo filter.`
**Overflow:** `📋 Showing top 20 of {N} results. Add vid filter or more specific terms to narrow.`
Error: "Query is required" | "Search service unavailable"

## Error Handling

| Error | Cause |
|-------|-------|
| Query is required | Empty search query |
| Repository not found | Invalid vid |
| Search service unavailable | DocSystem search is down |
| Too many results | Narrow query |

See [references/](references/) for related skills and API docs.
