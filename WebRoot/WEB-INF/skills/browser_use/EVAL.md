# browser_use — Baseline Evaluation

## Scores (dimension: raw × weight → points / max)

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 9/10 | 0.8 | 7.2/8 |
| 2 | Workflow clarity | 6/10 | 1.5 | 9.0/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 4/10 | 0.7 | 2.8/7 |
| 5 | Instruction specificity | 8/10 | 1.5 | 12.0/15 |
| 6 | Resource integration | 7/10 | 0.5 | 3.5/5 |
| 7 | Overall architecture | 7/10 | 1.5 | 10.5/15 |
| 8 | Actual performance | 8/10 | 2.5 | 20.0/25 |
| **TOTAL** | | | | **72.0/100** |

## Key strengths
- Frontmatter excellent: name规范, description含"AI-powered"触发词, comprehensive bilingual triggers
- Well-scoped parameter set: task (required), url (optional), max-steps with explicit bounds (default 10, max 50)
- Good error coverage: missing task, API key not configured, service down, timeout, bad URL
- Two practical examples showing English and Chinese task descriptions
- Highest frontmatter and instruction specificity scores across all 8 skills

## Key weaknesses
- HIGHEST RISK SKILL: autonomous web browsing — no confirmation checkpoint before taking actions on behalf of the user
- No guidance on step budget management (max-steps=10 is default but 15 was used in Example 2 — when should user increase it?)
- No explicit multi-site strategy: when a task spans multiple domains, no guidance on sequencing
- No step-count reporting or progress feedback loop defined in the skill
- Workflow lacks numbered steps
- references/ section not verified

## Dimension 8 dry-run notes

**Prompt 1:** "find the cheapest flight from Beijing to Tokyo"
Skill calls `docsys browser-use find the cheapest flight from Beijing to Tokyo`. Expected: AI searches web, returns cheapest flight with summary of steps. Skill handles this well. Caveat: no max-steps guidance — a complex search may need more than 10 steps. Score: 8/10.

**Prompt 2:** "帮我查一下从上海到深圳的火车票"
Skill calls `docsys browser-use 帮我查一下从上海到深圳的火车票`. Chinese language correctly forwarded. Expected: Chinese results for Shanghai-Shenzhen trains. Skill handles language matching implicitly via CLI forwarding. Score: 8/10.

**Prompt 3:** "compare prices of MacBook Pro M3 across Amazon and BestBuy"
Skill calls `docsys browser-use compare prices of MacBook Pro M3 across Amazon and BestBuy --max-steps 15`. Multi-site navigation is implied. The skill does not explicitly guide how to handle multi-site comparisons (navigate sequentially? parallel tabs? summarize per-site?). With 15 steps and 2 sites, this is tight but feasible. Score: 7/10 — slight deduction for lack of explicit multi-site strategy guidance.

## Suggested priority improvements (P0-P3)
1. P0: Checkpoint design — add mandatory confirmation before autonomous web browsing, especially for actions that submit forms, click ads, or navigate to potentially sensitive pages
2. P1: Workflow clarity — add numbered steps including: 1. Parse task, 2. Set starting URL (if provided), 3. Execute autonomous browsing loop, 4. Report results with step count
3. P1: Instruction specificity — add guidance on multi-site navigation strategy and step budget (when to recommend increasing max-steps)
4. P2: Boundary conditions — add guidance on what happens if the AI gets stuck in a loop (no-progress detection)
5. P3: Resource integration — verify references/ directory content
