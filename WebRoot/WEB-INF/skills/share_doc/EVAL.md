# share_doc — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 9/10 | 1.5 | 13.5/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 2/10 | 0.7 | 1.4/7 |
| 5 | Instruction specificity | 11/10 | 1.5 | 11.0/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 8/10 | 1.5 | 12.0/15 |
| 8 | Actual performance | 14/25 | 2.5 | 14.0/25 |
| **TOTAL** | | | | **61.2/100** |

## Key strengths
- Frontmatter is complete with rich Chinese and English triggers.
- Workflow clarity is excellent: the list-vs-create distinction is explicit and well-explained, two separate CLI commands are clearly differentiated, and the type/password parameter structure is well-defined.
- Error handling is the most comprehensive of the eight skills: five error cases covered including "Share creation failed" for API errors.
- Instruction specificity is strong: parameter types, requirements, and the password-sensitive output note ("Do not expose the actual password in plaintext output") are all good.
- The skill correctly handles the two distinct modes (list existing vs. create new) with separate commands.

## Key weaknesses
- **Checkpoint design is critically weak (2/10):** Creating a share link is potentially irreversible and can expose documents to the public internet. No confirmation is specified before creating a share link. The agent could create a public link for a private document without asking.
- **Example 3 shows hardcoded placeholders** (`<vid>`, `<docId>`, `<password>`) instead of explaining that the agent should prompt the user for these values.
- **Password prompting is underspecified:** The skill says "prompt for password if not provided" but does not describe the prompting mechanism or what to do if the user refuses to provide a password.
- **No link expiration or permission scope guidance:** No mention of expiring links, read-only vs. read-write access, or maximum share link counts.
- **References section is a stub.**

## Dimension 8 dry-run notes

**Test 1 — "list share links for document 123"**
Skill correctly identifies that vid is missing from the prompt. The skill does not explain what to do when vid is absent — it would likely default to vid=1 or fail. The test expects `docsys share-list` to be called but vid extraction is not described. Score: 3/5 — list mode correct, vid extraction gap.

**Test 2 — "create a password-protected share link for document 456"**
The skill correctly triggers share-create mode. It notes that the agent should prompt for password if not provided. However, the skill does not address what to do if the user refuses or cancels. The password should be prompted before calling the CLI command. The "do not expose plaintext password" note is present and correct. Score: 7/10 — correct mode, prompting logic partially described but incomplete.

## Suggested priority improvements (P0-P3)

1. **P0: Checkpoint design** — Add mandatory confirmation before creating a share link: specify the document being shared, the share type (public/password/private), and ask "Create this share link?" to prevent accidental exposure of private documents.
2. **P1: vid extraction when missing** — Add explicit guidance: if vid is not provided, call `docsys repos list` to find the document's repository, then resolve docId from the document name if needed.
3. **P1: Password prompting UX** — Describe the prompting mechanism for password (e.g., ask the user directly), what to do if the user declines, and how to present the resulting link securely.
4. **P2: Share link scope limits** — Add documentation on link expiration, access permissions, and any limits on the number of active share links per document.
5. **P3: Resource integration** — Fill references/ with actual share/link API documentation.
