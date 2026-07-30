# list_docs — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 7/10 | 0.8 | 5.6/8 |
| 2 | Workflow clarity | 7/10 | 1.5 | 10.5/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 3/10 | 0.7 | 2.1/7 |
| 5 | Instruction specificity | 9/10 | 1.5 | 13.5/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 10/10 | 1.5 | 15.0/15 |
| 8 | Actual performance | 17/10 | 2.5 | 17.0/25 |
| **TOTAL** | | | | **72.2/100** |

## Key strengths
- Frontmatter complete and well-formed.
- Excellent bilingual trigger list covering both EN and ZH, including "ls" and "browse" aliases.
- Parameter specification is the clearest of the evaluated skills: pid defaults to 0 (root) is explicitly stated.
- Output format clearly specifies folders listed first, then files, with name/size/date fields.
- Error table covers all 4 relevant error conditions.
- CLI aliases (`docsys ls`) are well-documented alongside the primary command.

## Key weaknesses
- Workflow lacks numbered steps and explicit input/output definitions per step.
- No checkpoint for large directory listings — no pagination guidance or "too many files" handling.
- vid must be provided; no guidance if the user references a repo by name rather than VID.
- Examples 1 and 2 use concrete values; Example 3 uses `<vid>` placeholder — inconsistent.
- `See [references/](references/)` is generic and likely unpopulated.

## Dimension 8 dry-run notes

**Test 1: "list documents in repository 1"**
- Skill extracts vid=1, pid defaults to 0 (root). Calls `docsys doc list 1`.
- Output format clearly specifies folders first, then files, with name/size/date.
- Score: 8/10 — clean extraction, default pid behavior is explicitly documented.

**Test 2: "列出文件夹 5 中的内容，仓库是 1" (Chinese: "list contents of folder 5, repo is 1")**
- Skill triggers on Chinese "列出文件夹内容". Extracts vid=1 and pid=5. Calls `docsys doc list 1 5`.
- Output format covers folder/file grouping correctly.
- Score: 9/10 — full parameter extraction including pid, correct Chinese trigger handling.

## Suggested priority improvements (P0-P3)

1. P0 (Boundary conditions — large directory): Add pagination guidance for repositories with many documents. E.g., if response indicates >100 items, suggest filtering by name or paging.
2. P1 (Actual performance — name-based repo reference): Add a note that if the user references a repository by name rather than VID, the skill should first call `docsys repos` to resolve the name to VID.
3. P2 (Workflow clarity): Number workflow steps with input/output for each step. Define that Step 1 output is {vid, pid}.
4. P3 (Resource integration): Populate `references/` with actual links to list_repos skill for VID resolution.
