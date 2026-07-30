# rename_doc — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 8/10 | 1.5 | 12.0/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 3/10 | 0.7 | 2.1/7 |
| 5 | Instruction specificity | 8/10 | 1.5 | 12.0/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 7/10 | 1.5 | 10.5/15 |
| 8 | Actual performance | 17/25 | 2.5 | 17.0/25 |
| **TOTAL** | | | | **68.5/100** |

## Key strengths
- Frontmatter is complete with comprehensive triggers including both English and Chinese variants.
- Two CLI command forms (`docsys doc rename`, `docsys mv`) add flexibility; the `mv` alias is correctly listed.
- Parameter table is detailed, including character restrictions for newName (max 255 chars, forbidden chars list).
- Error handling covers five cases including name conflict and invalid characters.
- Third example covers the `mv` alias which is a nice touch.

## Key weaknesses
- **Checkpoint design is critically weak (3/10):** Rename is a destructive operation that changes file identity. No confirmation step is specified; the agent could silently rename a document to an incorrect or conflicting name.
- **Example 3 vid is hardcoded as 1** when the user's prompt ("mv doc 789 to ...") does not provide a vid — this models incorrect behavior (guessing vid=1 rather than asking).
- **No character encoding guidance:** Chinese filenames are supported in examples but no guidance is given on URL-encoding or special character handling in the CLI call.
- **No rollback guidance:** If a rename causes a name conflict, the skill does not explain how to recover.
- **References section is a stub.**

## Dimension 8 dry-run notes

**Test 1 — "rename document 123 to new_report.pdf"**
Skill correctly extracts vid=1, docId=123, newName="new_report.pdf" and calls `docsys doc rename 1 123 new_report.pdf`. Returns confirmation with old and new name. Score: 5/5 — fully correct.

**Test 2 — "把文档 456 重命名为 2025年度报告.pdf，仓库2"**
Skill handles Chinese filename correctly, calls `docsys doc rename 2 456 2025年度报告.pdf`. The skill does not mention URL-encoding for special characters in filenames, but the example shows it works as-is. Score: 4/5 — minor gap on encoding.

**Test 3 — "mv doc 789 to updated_data.xlsx"**
The `mv` alias is correctly documented and Example 3 maps this to `docsys mv 1 789 updated_data.xlsx`. However, the vid=1 is guessed from nowhere (not present in the prompt) — the skill should instead prompt for the repository ID. Score: 8/15 — alias logic correct, but vid extraction from natural-language prompts not addressed.

## Suggested priority improvements (P0-P3)

1. **P0: Checkpoint design** — Add mandatory confirmation step: show the old name, the new name, and the target repository, then ask "Rename to [newName]?" before executing.
2. **P1: vid extraction from natural-language prompts** — Remove the hardcoded vid=1 from Example 3. Instead, add a note that if vid is not explicitly provided, the agent should first call `docsys repos list` to find available repositories and ask the user to confirm which one.
3. **P2: Character encoding note** — Add a note about handling special characters and spaces in filenames.
4. **P3: Rollback guidance** — Add a fallback section: if a rename fails or causes a conflict, how to recover (e.g., rename back to the original name).
