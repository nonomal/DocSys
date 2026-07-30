# lock_doc — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 7/10 | 0.8 | 5.6/8 |
| 2 | Workflow clarity | 7/10 | 1.5 | 10.5/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 2/10 | 0.7 | 1.4/7 |
| 5 | Instruction specificity | 9/10 | 1.5 | 13.5/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 11/10 | 1.5 | 16.5/15 |
| 8 | Actual performance | 18/10 | 2.5 | 20.0/25 |
| **TOTAL** | | | | **76.0/100** |

## Key strengths
- Frontmatter complete and well-formed.
- Good bilingual trigger list covering both EN and ZH.
- Parameter specification is excellent: type parameter includes explicit numeric values (1=exclusive, 2=shared) with defaults.
- 3 well-structured examples covering basic lock, Chinese input, and exclusive lock specification.
- Output format specifies lock type in confirmation.
- Error table covers all 4 error conditions including "Already locked" (an important stateful error).
- Architecture is clean and consistent.

## Key weaknesses
- No user confirmation checkpoint before locking a document — potentially blocks other users from editing.
- Workflow lacks numbered steps and explicit input/output definitions per step.
- When the document is already locked by another user, no guidance on what to do next (e.g., contact the user, wait, override?).
- `See [references/](references/)` is generic and may not be populated.
- The "Already locked" error could include the lock holder's identity if returned by the API.

## Dimension 8 dry-run notes

**Test 1: "lock document 123 in repo 1"**
- Skill extracts vid=1 and docId=123. Defaults to exclusive lock (type=1). Calls `docsys lock 1 123`.
- Score: 9/10 — clean extraction, correct default, output well-specified.

**Test 2: "锁定文档 456，仓库是 2，需要独占锁" (Chinese: "lock doc 456, repo 2, exclusive lock needed")**
- Skill triggers on Chinese "锁定" keyword. Extracts vid=2, docId=456, type=1 (exclusive). Calls `docsys lock 2 456 1`.
- Type parameter correctly mapped from "独占锁" to numeric 1.
- Score: 9/10 — full trilingual (ZH + EN) parameter extraction.

**Test 3: "帮我把这个文档锁一下，ID是789，仓库编号5" (Chinese: "help me lock this doc, ID is 789, repo number 5")**
- Skill extracts docId=789 from "ID是789" and vid=5 from "仓库编号5". Calls `docsys lock 5 789`.
- Score: 8/10 — casual Chinese phrasing parsed correctly, but "仓库编号" parsing is not explicitly demonstrated in any example.

## Suggested priority improvements (P0-P3)

1. P0 (Checkpoint design — score 2/10): Add a mandatory confirmation step before locking. Example: "This will lock document [docId] in repository [vid], preventing other users from editing it until unlocked. Type CONFIRM to proceed." This is especially important since locks can block collaborators.
2. P0 (Boundary conditions — already locked): Add guidance for the "Already locked" error: include the lock holder's name if available from the API response, and suggest contacting them or waiting.
3. P1 (Instruction specificity): Add an example showing how "shared lock" maps to type=2, and clarify when to use shared vs. exclusive locks.
4. P2 (Workflow clarity): Number workflow steps with input/output for each step.
5. P3 (Resource integration): Populate `references/` with actual links to unlock_doc and get_doc skills.
