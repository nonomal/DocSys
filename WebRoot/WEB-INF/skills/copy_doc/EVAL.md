# copy_doc — Baseline Evaluation

## Scores (dimension: raw × weight → points / max)

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 7/10 | 1.5 | 10.5/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 5/10 | 0.7 | 3.5/7 |
| 5 | Instruction specificity | 7/10 | 1.5 | 10.5/15 |
| 6 | Resource integration | 6/10 | 0.5 | 3.0/5 |
| 7 | Overall architecture | 7/10 | 1.5 | 10.5/15 |
| 8 | Actual performance | 7/10 | 2.5 | 17.5/25 |
| **TOTAL** | | | | **68.9/100** |

## Key strengths
- Frontmatter complete: name规范, description含"Copy"触发词, all required fields
- Good parameter specificity: docId, vid, targetPid (with 0=root convention clearly documented)
- Error handling covers 5 cases including "Insufficient storage" — a realistic edge case
- Auto-rename on conflict documented in output format
- CLI aliases (doc copy / cp) clearly shown
- Bilingual triggers

## Key weaknesses
- No numbered workflow steps with input/output per step
- No confirmation checkpoint before executing a write operation (copying a document)
- Example 3 ("cp doc 789 to folder backup") shows a placeholder `<folderId>` for a named folder — no guidance on how to resolve named folders to IDs
- targetPid=0 for root is documented but not explained where to find the root PID
- references/ section not verified

## Dimension 8 dry-run notes

**Prompt 1:** "copy document 123 to folder 5 in repo 1"
Skill correctly extracts vid=1, docId=123, targetPid=5. Calls `docsys doc copy 1 123 5`. Expected: copy result with new document ID. Skill handles this cleanly. Score: 8/10.

**Prompt 2:** "cp doc 789 to folder backup"
Skill recognizes "cp" alias, extracts docId=789. However, "folder backup" is a named folder, not a numeric PID. The skill has no guidance on resolving folder names to IDs — Example 3 shows `<folderId>` placeholder. Without resolution logic, this would fail or require user input. Score: 5/10 — significant gap: named folder to ID resolution not addressed.

**Prompt 3:** "复制文档 456 到根目录"
Skill correctly maps "根目录" to targetPid=0. Calls `docsys doc copy <vid> 456 0`. vid is missing in the example (placeholder `<vid>`). Should prompt for vid. Score: 6/10.

## Suggested priority improvements (P0-P3)
1. P0: Checkpoint design — add confirmation step before copying (show source doc name, target folder, warn if auto-rename will occur)
2. P1: Instruction specificity — add guidance on resolving named folders to numeric PIDs (e.g., call `docsys doc list <vid>` to find folder IDs by name)
3. P2: Workflow clarity — add numbered steps with input/output per step
4. P3: Resource integration — verify references/ directory content
