# unlock_doc — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 6/10 | 1.5 | 9.0/15 |
| 3 | Boundary conditions | 9/10 | 1.0 | 9.0/10 |
| 4 | Checkpoint design | 7/10 | 0.7 | 4.9/7 |
| 5 | Instruction specificity | 8/10 | 1.5 | 12.0/15 |
| 6 | Resource integration | 6/10 | 0.5 | 3.0/5 |
| 7 | Overall architecture | 8/10 | 1.5 | 12.0/15 |
| 8 | Actual performance | 6/10 | 2.5 | 15.0/25 |
| **TOTAL** | | | | **72.3/100** |

## Key strengths
- Error handling table is the most comprehensive of all 9 skills — covers 5 distinct error cases with specific causes
- Triggers are very thorough (bilingual, 12+ variants including "unprotect", "remove lock")
- Parameters section clearly specifies vid (number, required) and docId (number, required)
- CLI syntax is unambiguous
- "Cannot unlock" case covers lock-owner restriction

## Key weaknesses
- Examples 2 and 3 use `<vid>` and `<docId>` placeholders instead of actual values, which may confuse agents about whether these are required
- Example 2 Chinese: `docsys unlock <vid> 456` — the vid placeholder suggests the agent must ask for it, but the test expects this behavior and SKILL.md does not explicitly tell the agent to ask
- No explicit workflow step for "ask user for missing vid" when the user does not provide it
- No confirmation checkpoint before unlocking (other users may be relying on the lock)
- Error "Not locked" exits with error; test expects "does not error if already unlocked" — this is a gap

## Dimension 8 dry-run notes

**Test 1 — "unlock document 789 in repo 3":**
Skill triggers. Extracts vid=3, docId=789 from prompt. Calls `docsys unlock 3 789`. Example 1 matches this pattern exactly. **Score: 8/10**

**Test 2 — "解除文档 456 的锁定":**
Skill triggers (Chinese). vid is not in the prompt — only docId (456) is provided. Test expectation: "skill should ask for [vid] before calling." SKILL.md Example 2 uses `<vid>` placeholder but does not explicitly instruct the agent to prompt the user for vid. The Parameters table marks vid as required, but there is no guidance on what to do when the user omits it. The error handling table says "Usage: unlock <vid> <docId>" for missing arguments, but that would result in an error rather than a helpful prompt. **Score: 5/10**

**Without skill:** Agent might guess the command syntax or fail to include both required arguments. Skill provides the syntax and error handling. Skill quality difference is meaningful for Test 1, but insufficient for Test 2.

## Suggested priority improvements (P0-P3)
1. P0: Add explicit workflow step for missing required parameters: "If vid or docId is not provided in the user prompt, ask the user to supply it before calling the CLI"
2. P0: Address the "Not locked" error vs. test expectation discrepancy — either change Error Handling to say "Not locked: exits cleanly" or add guidance that this is not an error
3. P1: Replace `<vid>` and `<docId>` placeholders in examples with actual values or explicitly note they are required parameters to extract from context
4. P2: Add a confirmation checkpoint before unlocking ("This document is currently locked by another user. Proceed?")
