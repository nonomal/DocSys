---
name: browser_use
description: AI-powered web browsing via browser-use.com API
category: web
version: 1.1.0
author: DocSys Team
permissions:
  - read
tags: [web, browser, ai, automation, agent, intelligent]
---

# Browser Use AI

AI-powered web browsing using browser-use.com API. AI autonomously navigates web pages to complete tasks described in natural language.

## Triggers

Browser use, ai-browse, 智能浏览, ai browsing, ai-browser, 智能爬虫, AI 浏览器, autonomous browser, intelligent browsing, AI 自动浏览网页, 智能网页操作, 帮我查一下

## Workflow

1. **Parse task**: Extract natural language task, optional URL, and optional max-steps.
2. **⚠️ Confirm before browsing**: Always confirm when: (a) URL is explicitly provided, (b) task involves submitting forms, (c) clicking ads or making purchases, (d) navigating to sensitive/auth sites, (e) multi-site research. Show task summary and URL before proceeding. If user declines → stop.
3. **Set step budget**: Default=10. For multi-site tasks or complex research → recommend 15-20. For very complex tasks (50+ pages) → set 30-50.
4. **Call CLI**: `docsys browser-use <task> [--url <url>] [--max-steps N]`
5. **Report with step count**: Show results + steps executed. If approaching step limit → warn user.
6. **No-progress detection**: If AI seems stuck (same URL repeated) → stop and report partial results.

## CLI Command

```bash
docsys browser-use <task> [--url <url>] [--max-steps 10]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| task | string | yes | Natural language description of browsing task |
| url | string | no | Starting URL (AI searches if not provided) |
| max-steps | number | no | Max browser steps (default: 10, max: 50) |

## Examples

### Example 1
User: "find the cheapest flight from Beijing to Tokyo"
Skill triggers → calls: `docsys browser-use find the cheapest flight from Beijing to Tokyo`
Output: AI-navigated results

### Example 2
User: "compare prices of MacBook Pro M3 across Amazon and BestBuy"
Skill triggers → calls: `docsys browser-use compare prices of MacBook Pro M3 across Amazon and BestBuy --max-steps 15`
Output: AI comparison results

### Example 3
User: "帮我查一下从上海到深圳的火车票"
Skill triggers → Step 2: no explicit URL → no confirm needed → Step 3: default step budget=10 → calls: `docsys browser-use 帮我查一下从上海到深圳的火车票 --max-steps 10`
Output: AI 浏览结果 + steps executed

### Example 4 (complex multi-site research)
User: "research all cloud providers and compare their pricing"
Skill triggers → Step 2: ⚠️ multi-site research → must confirm before proceeding → shows task summary → user confirms → Step 3: complex task → recommends --max-steps 20-30 → calls: `docsys browser-use research all cloud providers and compare their pricing --max-steps 25`
Output: Comparison results + steps executed + partial results if approaching step limit

### Example 5 (stall detection)
User: "find the latest news about AI"
Skill triggers → calls: `docsys browser-use find the latest news about AI --max-steps 10`
If AI reports same URL 3 times → Step 6: no-progress detected → stop early and report partial results with warning: "Stopped early: AI appeared stuck at [URL]. Partial results shown above."

## Output Format

Success: AI-generated results from autonomous browsing, with steps executed count.
Error: "Task is required", "API key not configured", "Task timeout"

## Error Handling

| Error | Cause |
|-------|-------|
| Task is required | Empty task description |
| API key not configured | No browser-use.com key |
| Browser-use API unavailable | Service down |
| Task timeout | Exceeded time limit |
| Navigation failed | Bad URL or blocked site |

See [references/](references/) for related skills and API docs.
