# playwright — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 8/10 | 1.5 | 12.0/15 |
| 3 | Boundary conditions | 5/10 | 1.0 | 5.0/10 |
| 4 | Checkpoint design | 3/10 | 0.7 | 2.1/7 |
| 5 | Instruction specificity | 8/10 | 1.5 | 12.0/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 7/10 | 1.5 | 10.5/15 |
| 8 | Actual performance | 13/25 | 2.5 | 13.0/25 |
| **TOTAL** | | | | **62.5/100** |

## Key strengths
- Frontmatter is complete with Chinese triggers.
- Workflow is well-structured with clear CLI commands and a parameter table covering all five operations (open, screenshot, click, fill, close).
- CSS selector syntax is explicitly called out in parameter descriptions.
- Three examples cover the main use cases clearly.

## Key weaknesses
- **No operation chaining guidance:** Playwright workflows are inherently sequential (open → fill → click → screenshot). The skill lists operations side-by-side but never explains how to chain them, leading to failed multi-step test cases.
- **Missing key selectors:** Test 3 requires `BAIDU_SEARCH_INPUT` and `BAIDU_SUBMIT_BUTTON` but the skill provides no guidance on common Chinese website selector patterns.
- **No waiting/timeout guidance:** No mention of implicit waits, explicit waits, or retry strategies when elements are not immediately available.
- **Checkpoint design is minimal (3/10):** No confirmation before opening arbitrary URLs or taking screenshots that may contain sensitive content.
- **Error handling is incomplete:** No guidance on navigation errors, network failures, or popup dialogs.

## Dimension 8 dry-run notes

**Test 1 — "open https://example.com in browser and take a screenshot"**
Skill calls `docsys playwright open https://example.com` then `docsys playwright screenshot`. Both commands are described separately but not explicitly chained. Score: 4/5 — mostly correct, chaining implicit rather than explicit.

**Test 2 — "帮我截个图，当前页面"**
The skill's `screenshot` command takes no URL parameter and works on the current page. This aligns perfectly with the test expectation. Score: 5/5 — correct.

**Test 3 — "打开百度，搜索 Claude AI，然后把结果截图"**
This is the hardest case. The skill provides individual commands but does NOT explain:
1. How to chain open → fill → click → screenshot as a sequence.
2. What selectors to use for baidu.com (no common input/form selectors given).
3. How to wait for search results to load before screenshot.
The skill as written would likely call `docsys playwright open baidu.com` and stop, not continuing the chain. Score: 4/15 — significant gap.

## Suggested priority improvements (P0-P3)

1. **P0: Operation chaining workflow** — Add a dedicated "Multi-step Workflow" section explaining the ordered sequence: open → wait → fill → click → wait → screenshot, with explicit state expectations between steps.
2. **P1: Selector guidance** — Add common selector patterns for input forms, buttons, and dynamic content. Include a timeout/wait strategy section.
3. **P1: Error handling expansion** — Cover navigation errors, element-not-found errors, popup dialogs, and retry strategies.
4. **P2: Checkpoint design** — Add a warning/best-practice note about confirming URL before opening and avoiding screenshots of sensitive content.
5. **P3: Resource integration** — Fill references/ with actual Playwright API docs or a selector guide.
