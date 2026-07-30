---
name: web_search
description: Search the web using search engines (Baidu, Google, Bing) with result ranking and snippet extraction
category: search
version: 1.2.0
author: DocSys Team
permissions:
  - read
tags: [web, search, search-engine, baidu, google, research]
---

# Web Search

Search the web using search engines when local documents don't have relevant results. Supports multi-engine search, result ranking, snippet extraction, and citation guidance.

## Triggers

Web search, 网络搜索, 网上搜索, 百度搜索, search the web, web-search, search web, 搜索网页, search online, google search, 网上查, 在线搜索, 搜索互联网, 帮我查一下, 帮我搜一下, look up online, find on the internet

## Workflow

1. **Validate & reformulate query**: Ensure query is non-empty. If vague or ambiguous → try to reformulate before searching (e.g., "latest iPhone" → "iPhone latest release date 2024"). If truly empty → report error.
2. **Determine engine**: Infer from keywords ("百度" / "中文" → baidu, "google" / "英语" → google, "英文资料" → google/bing). Default: baidu. For international queries → recommend google.
3. **⚠️ Sensitive content check**: If query involves medical/financial/legal advice → add disclaimer: "以下信息仅供参考，不构成专业建议。"
4. **Call CLI**: `docsys web-search <query> [--engine <engine>] [--limit <N>]`
5. **Process results**: For each result, extract: title (bold), URL, and a 1-2 sentence snippet showing why this result is relevant. Remove duplicate domains.
6. **Rank and present**: Order by relevance (title match > snippet relevance > domain authority). Show top 5-10 results with snippets.
7. **Rate limit handling**: If rate limited → wait 30s and retry once. If still blocked → suggest trying a different engine.
8. **Citation guidance**: After presenting results, optionally include: "来源: [title](URL)" for each result the user references.
9. **Next steps**: If 0 results → suggest broadening query or trying a different engine. If few results → suggest related searches.

## CLI Command

```bash
docsys web-search <query> [--engine baidu] [--limit 10]
```

> **Tip**: For academic queries → use `--engine google`. For Chinese queries → baidu is best. For real-time stock/price data → results are snapshots only, not live data.

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| query | string | yes | Search query |
| engine | string | no | Search engine: baidu (default), google, bing |
| limit | number | no | Max results (default: 10, max: 50) |

## Examples

### Example 6 (Tip: engine selection)
User: "find academic papers on quantum computing"
Skill triggers → Step 2: academic query → recommends `--engine google` → calls → results
Output: Academic papers with snippets. Tip shown: "For academic queries → use --engine google."

### Example 7 (Local-first guidance)
User: "search for project notes about Q3 planning"
Skill triggers → Before searching: "This query sounds like it might be in your local repositories. Try `search_doc` or `search_in_repo` first. If no local results → proceed with web search."
Output: Web search results only (after local search suggestion)


User: "search the web for Claude AI features"
Skill triggers → Step 1: query clear → Step 2: engine=baidu (default) → Step 3: no sensitive content → calls: `docsys web-search Claude AI features --limit 10`
Step 5: extracts snippets from each result. Step 6: presents top results ranked.
Output: [Result 1] Claude AI Official Site — https://claude.ai — Anthropic's next-generation AI assistant...
[Result 2] Claude AI Features Overview — https://docs.anthropic.com — Comprehensive guide to Claude capabilities...

### Example 2
User: "百度搜索 人工智能发展趋势"
Skill triggers → Step 1: query clear → Step 2: engine=baidu (inferred) → Step 3: no sensitive content → calls: `docsys web-search 人工智能发展趋势 --engine baidu --limit 10`
Output: 搜索结果列表，含标题、URL、摘要

### Example 3
User: "帮我查一下 特斯拉最新股价"
Skill triggers → Step 1: query clear → Step 2: engine=baidu → Step 3: ⚠️ financial content → adds disclaimer → calls: `docsys web-search 特斯拉最新股价 --limit 5`
Output: Results + "⚠️ 股价信息仅供参考，不构成投资建议。"

### Example 4 (no results → next steps)
User: "search for xyzabc123 nonexistent query"
Skill triggers → calls: `docsys web-search xyzabc123 --limit 10` → 0 results → Step 9: suggests broadening query or trying different engine
Output: No results found. Try: (1) broader keywords, (2) different engine (e.g. --engine google), (3) check spelling.

### Example 5 (multi-engine)
User: "find academic papers on machine learning"
Skill triggers → Step 1: query reformulated → Step 2: suggests --engine google (academic) → calls → presents ranked results
Output: Academic results with snippets showing relevance to machine learning

## Output Format

Success:
```
[Result 1] <title> — <URL>
  摘要: <1-2 sentence snippet explaining relevance>

[Result 2] <title> — <URL>
  摘要: <...>
```
⚠️ Sensitive queries add disclaimer: "以下信息仅供参考，不构成XX建议。"

Zero results: "No results found. Suggestions: (1) broaden keywords, (2) try --engine google, (3) check spelling."

Error: "Query is required", "Browser not available", "Rate limit exceeded"

## Error Handling

| Error | Cause |
|-------|-------|
| Query is required | Empty query |
| Browser not available | Playwright not initialized |
| Search engine blocked | Anti-bot protection |
| Rate limit exceeded | Too many searches |
| Connection timeout | Network issue |
| Zero results | Query too specific or engine blocked |

## Related Skills

If user searches for content that may be in local repos → suggest `search_doc` or `search_in_repo` first.

See [references/](references/) for related skills and API docs.
