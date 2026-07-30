# delete_doc — Baseline Evaluation

## Scores (dimension: raw × weight → points / max)

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 7/10 | 1.5 | 10.5/15 |
| 3 | Boundary conditions | 8/10 | 1.0 | 8.0/10 |
| 4 | Checkpoint design | 3/10 | 0.7 | 2.1/7 |
| 5 | Instruction specificity | 7/10 | 1.5 | 10.5/15 |
| 6 | Resource integration | 6/10 | 0.5 | 3.0/5 |
| 7 | Overall architecture | 7/10 | 1.5 | 10.5/15 |
| 8 | Actual performance | 8/10 | 2.5 | 20.0/25 |
| **TOTAL** | | | | **71.0/100** |

## Key strengths
- Frontmatter complete: name规范, description含"Delete"触发词, includes "destructive" tag — good risk awareness
- Best boundary condition coverage of the 8 skills: 6 error cases including "Cannot delete system document" (protection awareness)
- Parameter specificity good: vid + docId clearly required
- CLI aliases (doc delete / rm) shown
- Bilingual triggers

## Key weaknesses
- CRITICAL: No confirmation checkpoint before permanent deletion — this is the most destructive operation in the skill set
- "destructive" tag in frontmatter is a good start but no actual safeguard in the workflow
- No numbered workflow steps with input/output per step
- No guidance on whether deletion is reversible (trash/soft-delete vs hard delete)
- No warning to the user about the permanence of the action
- references/ section not verified

## Dimension 8 dry-run notes

**Prompt 1:** "delete document 789 from repo 1"
Skill correctly extracts vid=1, docId=789. Calls `docsys doc delete 1 789`. Expected: brief deletion confirmation. Skill handles this correctly from a parsing perspective. Score: 8/10 — parsing is correct but no confirmation safeguard.

**Prompt 2:** "永久删除仓库 2 中的文档 456"
Skill triggers on Chinese phrase, extracts vid=2, docId=456. Calls `docsys doc delete 2 456`. "永久删除" (permanent delete) explicitly stated by user — but the skill does not acknowledge this or add any extra warning. Score: 7/10 — extraction is correct; the skill misses the opportunity to add a checkpoint for explicitly permanent deletion requests.

## Suggested priority improvements (P0-P3)
1. P0: Checkpoint design — MANDATORY confirmation step before deletion: "You are about to permanently delete document <docId> from repo <vid>. This cannot be undone. Type 'delete' to confirm." This is the single most critical improvement across all 8 skills.
2. P1: Workflow clarity — add numbered steps with explicit input/output per step
3. P2: Boundary conditions — clarify whether deletion is permanent or goes to trash; add "document is in system-protected location" warning
4. P3: Resource integration — verify references/ directory content
