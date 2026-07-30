# search_doc — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 8/10 | 1.5 | 12.0/15 |
| 3 | Boundary conditions | 6/10 | 1.0 | 6.0/10 |
| 4 | Checkpoint design | 5/10 | 0.7 | 3.5/7 |
| 5 | Instruction specificity | 11/15 | 1.5 | 11.0/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 11/15 | 1.5 | 11.0/15 |
| 8 | Actual performance | 17/25 | 2.5 | 17.0/25 |
| **TOTAL** | | | | **63.0/100** |

## Key strengths
- Frontmatter is complete with extensive Chinese and English triggers covering all common search verbs.
- Workflow is clear and consistent with the search_in_repo sibling skill.
- Parameter table includes the optional vid filter for repo-specific search.
- Error handling covers four cases including the "Too many results" case.
- Output format specifies the fields to return per result (repository, filename, path, size, date).
- Search is a read-only operation, so checkpoint design is less critical (reflected in 5/10 rather than a lower score).

## Key weaknesses
- **No guidance on result pagination or limiting:** "Too many results" is mentioned as an error but no strategy is given for how to handle or present them.
- **No query refinement guidance:** If zero results are found, no fallback strategy is described (e.g., try shorter keywords, remove filters).
- **No search syntax guidance:** No mention of boolean operators, exact phrase matching, or wildcard support.
- **Multi-keyword query handling is ambiguous:** Example 3 uses a natural-language query ("find machine learning papers") but the CLI syntax for multi-word queries is not specified (e.g., should it be quoted?).
- **Chinese result formatting is implied but not specified.**
- **References section is a stub.**

## Dimension 8 dry-run notes

**Test 1 — "search for project documents"**
Skill calls `docsys search project`. Returns a list with repository name, filename, path, size, date for each result. Score: 5/5 — fully correct.

**Test 2 — "搜索包含 Q4 报告的文档"**
Skill calls `docsys search Q4 报告`. Chinese keywords are passed correctly. Output format is implied to be Chinese. Score: 5/5 — fully correct.

**Test 3 — "find machine learning papers across all my repos"**
The phrase "across all my repos" clarifies global search intent (no vid filter needed). The skill calls `docsys search machine learning papers` — the multi-word query is passed as separate arguments, which may or may not be correct depending on CLI implementation. The skill does not specify how multi-word queries should be quoted or joined. Score: 7/15 — correctly triggers, but multi-keyword query formatting is ambiguous.

## Suggested priority improvements (P0-P3)

1. **P1: Query formatting guidance** — Clarify how multi-word queries should be passed to the CLI (single string vs. space-separated). Add an example of a quoted phrase search.
2. **P1: Zero-results and overflow guidance** — Add a fallback section: if zero results, suggest broadening the query. If too many results, suggest adding a repository filter or more specific terms.
3. **P2: Search syntax** — Add a note on any supported search operators (AND, OR, phrase matching with quotes).
4. **P2: Result presentation** — Specify pagination behavior (return top N results, sorted by relevance) and whether to show per-repository grouping.
5. **P3: Resource integration** — Fill references/ with search service documentation.
