# search_in_repo — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 7/10 | 1.5 | 10.5/15 |
| 3 | Boundary conditions | 6/10 | 1.0 | 6.0/10 |
| 4 | Checkpoint design | 4/10 | 0.7 | 2.8/7 |
| 5 | Instruction specificity | 9/10 | 1.5 | 13.5/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 7/10 | 1.5 | 10.5/15 |
| 8 | Actual performance | 13/25 | 2.5 | 13.0/25 |
| **TOTAL** | | | | **59.7/100** |

## Key strengths
- Frontmatter is complete with comprehensive Chinese and English triggers including natural phrasing ("在这个仓库里找", "find in repo").
- Parameter table is clear: query (required) and vid (required) are both well-defined.
- Chinese and English examples are both provided.
- The skill correctly scopes searches to a specific repository, which is a meaningful differentiation from search_doc.
- Instruction specificity is strong (9/10): parameter types, requirements, and formats are all clear.

## Key weaknesses
- **Workflow clarity gap (7/10):** Unlike search_doc, there is no output format specification section. The user does not know what fields will be returned.
- **Natural-language repo references are unresolved:** Test 3 ("search project repo for architecture files") requires translating "project repo" into a numeric vid. The skill gives no guidance on how to do this — it silently falls through to guessing or failing.
- **No query refinement or zero-results guidance:** If the search returns no results, the skill does not suggest broadening or reformulating the query.
- **No pagination or result limit guidance.**
- **Checkpoint design: repo-scoped search is read-only so lower risk, but still no guidance** on whether to confirm before showing potentially sensitive search terms in logs.
- **References section is a stub.**

## Dimension 8 dry-run notes

**Test 1 — "search for reports in repository 1"**
Skill correctly extracts query="reports" and vid=1, calls `docsys search reports 1`. Results scoped to repo 1 only. Score: 5/5 — fully correct.

**Test 2 — "在仓库 2 中搜索机器学习相关文档"**
Skill correctly parses Chinese input, extracts query="机器学习" and vid=2, calls `docsys search 机器学习 2`. Chinese results expected. Score: 5/5 — fully correct.

**Test 3 — "search project repo for architecture files"**
The phrase "project repo" is a natural-language reference to a repository name, not a numeric vid. The skill provides no guidance on how to resolve repository names to IDs. It would likely either fail (vid required but not found) or guess. The test explicitly requires the skill to "resolve it to a vid or ask user to clarify." Neither behavior is described in SKILL.md. Score: 3/15 — major gap in natural-language vid extraction.

## Suggested priority improvements (P0-P3)

1. **P0: Natural-language vid resolution** — Add a section explaining that if the user references a repository by name (e.g., "project repo") instead of a numeric ID, the agent must first call `docsys repos list` to find matching repository names, then map to a vid. Include this as a mandatory pre-step.
2. **P1: Output format specification** — Add a section describing what fields are returned per result (at minimum: filename, path, size, date, relevance indicator).
3. **P2: Zero-results and overflow guidance** — Add fallback advice when no results are found (suggest broader terms, remove filters).
4. **P2: Chinese result formatting** — Explicitly specify that Chinese queries should return Chinese-formatted output.
5. **P3: Resource integration** — Fill references/ with search service documentation.
