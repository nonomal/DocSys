# whoami — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 6/10 | 0.8 | 4.8/8 |
| 2 | Workflow clarity | 7/10 | 1.5 | 10.5/15 |
| 3 | Boundary conditions | 6/10 | 1.0 | 6.0/10 |
| 4 | Checkpoint design | 10/10 | 0.7 | 7.0/7 |
| 5 | Instruction specificity | 7/10 | 1.5 | 10.5/15 |
| 6 | Resource integration | 6/10 | 0.5 | 3.0/5 |
| 7 | Overall architecture | 8/10 | 1.5 | 12.0/15 |
| 8 | Actual performance | 8/10 | 2.5 | 20.0/25 |
| **TOTAL** | | | | **73.8/100** |

## Key strengths
- Triggers list is the longest and most comprehensive of all skills (bilingual, many English and Chinese variants)
- Example 3 in SKILL.md explicitly covers the error scenario (not logged in) with concrete output
- Error handling table covers 3 cases with distinct causes
- Read-only command; checkpoint design is naturally safe (no confirmations needed)

## Key weaknesses
- Missing `tags` field in frontmatter (only skill of the 9 without it)
- Error handling table lacks explicit recovery steps (e.g., what to do on "Session expired")
- Examples 3 and 4 output strings that appear artificial ("错误：Not logged in") rather than what a real CLI would return
- Single-step workflow; no numbered steps or structured workflow section

## Dimension 8 dry-run notes

**Test 1 — "who am I":**
Skill triggers (whoami in triggers list). Calls `docsys whoami`. Returns username, role, permissions, login_time, session_status. Expected output format in SKILL.md Output Format section matches expected behavior closely. Matches test expectation. **Score: 8/10**

**Test 2 — "am I logged in?":**
Skill triggers (English question variant "am I logged in" is in triggers list). Calls `docsys whoami`. SKILL.md Error Handling says "Not logged in" for no active session, "Session expired" for token invalid. The test expects clear "Not logged in" message and distinction from session expired. SKILL.md does list both error strings separately, so a well-implemented agent can return the right one. **Score: 8/10**

**Without skill:** An agent would need to guess the correct CLI command and output format. The skill provides it unambiguously. Skill quality difference is significant.

## Suggested priority improvements (P0-P3)
1. P0: Add `tags: [user, identity, session, auth]` to frontmatter (frontmatter is the lowest-scoring dimension at 6/10)
2. P1: Add a Workflow section with numbered steps (1. Verify session 2. Call docsys whoami 3. Format response) to improve workflow clarity
3. P2: Add recovery guidance to error handling table (e.g., "Session expired → re-run docsys login")
4. P2: Make example outputs realistic (e.g., actual JSON or key=value format instead of pseudo-error strings)
