# move_doc — Baseline Evaluation

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
- Frontmatter is complete: name, description, Chinese+English triggers, all under limits.
- Workflow is well-structured: CLI command, parameter table, three examples (EN/CN/mixed), output format, error table.
- Error handling table covers the five most common failure modes (missing args, not found x2, already there, permission denied).
- Third test prompt (named folder "Archive") is acknowledged and explicitly called out as requiring folderId resolution.

## Key weaknesses
- **Checkpoint design is the biggest gap (3/10):** No user confirmation before executing a destructive move. An agent could silently move a document to the wrong folder without asking.
- **Named folder handling is unresolved:** Example 3 shows placeholders (`<vid>`, `<folderId>`) instead of concrete guidance on how to resolve "Archive" to a folderId.
- **No rollback/recovery path:** If the move fails mid-operation, no cleanup or retry guidance is provided.
- **References section is empty** (only a path stub, no actual content).

## Dimension 8 dry-run notes

**Test 1 — "move document 123 to folder 5 in repo 1"**
Skill correctly extracts vid=1, docId=123, targetPid=5 and calls `docsys doc move 1 123 5`. Would return from/to confirmation. Score: 5/5 — fully correct.

**Test 2 — "把文档 456 移动到根目录，仓库是 2"**
Skill correctly interprets targetPid=0 for root directory, calls `docsys doc move 2 456 0`. Chinese confirmation expected. Score: 5/5 — fully correct.

**Test 3 — "move file 789 to folder Archive"**
Named folder reference "Archive" requires a folderId lookup that is not described. The skill shows placeholders but does not explain how to resolve a folder name to an ID. The skill explicitly notes "does not silently fail or guess incorrect ID" in test expectations, but the SKILL.md itself gives no mechanism for that resolution. Score: 7/15 — partial guidance present but mechanism missing.

## Suggested priority improvements (P0-P3)

1. **P0: Checkpoint design** — Add a required confirmation step before executing move: present the parsed parameters (from location, to location, doc name) and ask "Proceed with this move?" to prevent autonomous mis-moves.
2. **P1: Named folder resolution** — Add a step in the workflow to call `docsys folder list <vid>` to resolve folder names to IDs, with an example. Replace placeholders in Example 3 with concrete values.
3. **P2: Resource integration** — Fill in the `references/` directory with actual API docs and related skill links.
4. **P3: Rollback guidance** — Add a fallback/recovery section explaining how to undo a move (call `docsys doc move vid docId originalPid`) if the user reports an error.
