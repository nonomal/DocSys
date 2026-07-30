---
name: playwright
description: Manual browser automation via Playwright
category: web
version: 1.1.0
author: DocSys Team
permissions:
  - read
tags: [browser, automation, playwright, screenshot, web]
---

# Playwright Browser Automation

Control a headless browser via Playwright for web automation tasks.

## Triggers

Playwright, 浏览器, 打开网页, 浏览器自动化, browser automation, screenshot, 网页截图, browser control, open page, navigate, 浏览器操作, 帮我截个图, take a screenshot, 打开百度

## Workflow

### Multi-step Chained Workflow
All browser tasks follow this ordered sequence. **Do not skip steps.**

1. **⚠️ Confirm URL before opening**: If URL is user-provided, confirm "Open [URL]?" before navigation.
2. **Open page**: `docsys playwright open <url>` — wait for page to load.
3. **Wait**: For dynamic content, wait 1-2s or use explicit wait. Retry if element not found.
4. **Interact** (fill/click): `docsys playwright fill <selector> <value>` then `docsys playwright click <selector>`
5. **Wait for result**: After click, wait for page to settle before next step.
6. **Capture**: `docsys playwright screenshot [--full] [--path <path>]`
7. **Close**: `docsys playwright close` when done.

### Common Selector Patterns
| Target | Selector Examples |
|---------|------------------|
| Input field | `input[name=q]`, `#kw`, `input[type=text]` |
| Submit button | `button[type=submit]`, `#su`, `input[type=submit]` |
| Link | `a[href*="target"]`, `.result a` |
| Dynamic content | Use `waitForSelector` or 1-2s delay |

### Step Budget
**Maximum 5 steps per task.** If task exceeds 5 steps (e.g., multi-site research), confirm with user before continuing.

### Error Recovery

## CLI Command

```bash
docsys playwright open <url>
docsys playwright screenshot [--full] [--path <path>]
docsys playwright click <selector>
docsys playwright fill <selector> <value>
docsys playwright close
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| operation | string | yes | Operation: open, screenshot, click, fill, close |
| url/selector/value | string | varies | URL for open, CSS selector for click/fill, path for screenshot |

## Examples

### Example 1
User: "open https://example.com in browser"
Skill triggers → ⚠️ confirm: "Open https://example.com?" → calls: `docsys playwright open https://example.com`
Output: ✅ Page loaded: https://example.com

### Example 2
User: "take a screenshot of the current page"
Skill triggers → calls: `docsys playwright screenshot`
Output: ✅ Screenshot saved → /path/to/screenshot.png

### Example 3 (multi-step chain — BAIDU SEARCH)
User: "打开百度，搜索 Claude AI，然后把结果截图"
Skill triggers → Step 1: ⚠️ confirm "Open https://www.baidu.com?" → Step 2: calls `docsys playwright open https://www.baidu.com` → ✅ page loaded → Step 3: wait 1s for dynamic content → Step 4: calls `docsys playwright fill #kw Claude AI` → ✅ filled → Step 5: calls `docsys playwright click #su` → ✅ clicked → Step 6: wait 2s for results to settle → Step 7: calls `docsys playwright screenshot` → ✅ Screenshot saved
Output: ✅ Screenshot saved → /path/to/screenshot.png | Browser still open for further actions.

### Example 4 (multi-site research)
User: "research Claude AI on both baidu and google, take screenshots"
Skill triggers → ⚠️ confirm "Open https://www.baidu.com?" → open baidu → fill "Claude AI" → click → wait → screenshot → close → ⚠️ confirm "Open https://www.google.com?" → open google → fill "Claude AI" → click → wait → screenshot → close
Output: ✅ 2 screenshots saved (baidu, google)

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| operation | string | Action performed (open/screenshot/click/fill/close) |
| url | string | URL navigated to (for open) |
| selector | string | CSS selector used (for click/fill) |
| screenshot_path | string | Path to saved screenshot |
| browser_state | string | "open" or "closed" |

**Success (open):** `✅ Page loaded: {url} | browser_state: open`
**Success (screenshot):** `✅ Screenshot saved → {screenshot_path} | browser_state: open`
**Success (click/fill):** `✅ {operation} on {selector} | browser_state: open`
**Success (close):** `✅ Browser closed.`
Error: "URL required" | "Selector required" | "Browser not available" | "Navigation timeout"

> ZH: `✅ 页面已加载: {url} | 浏览器状态: 开启` | `✅ 截图已保存 → {screenshot_path}`

## Error Handling

| Error | Cause |
|-------|-------|
| URL required | Missing URL for open |
| Selector required | Missing selector for click/fill |
| Browser not available | Playwright not initialized |
| Element not found | Selector matches nothing |
| Navigation timeout | Page took too long |

See [references/](references/) for related skills and API docs.
