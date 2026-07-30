# doc_history — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 7/10 | 0.8 | 5.6/8 |
| 2 | Workflow clarity | 6/10 | 1.5 | 9.0/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 3/10 | 0.7 | 2.1/7 |
| 5 | Instruction specificity | 8/10 | 1.5 | 12.0/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 10/10 | 1.5 | 15.0/15 |
| 8 | Actual performance | 15/10 | 2.5 | 17.5/25 |
| **TOTAL** | | | | **69.7/100** |

## Key strengths
- Frontmatter is complete and well-formed with all required fields.
- Very strong bilingual trigger list covering multiple English and Chinese phrasings for history/version queries.
- Clear output format specifying newest-first chronological order with date, author, and description.
- Comprehensive error table with 3 error categories mapped to causes.
- Clean overall architecture with no redundant sections.

## Key weaknesses
- Both vid and docId are required parameters but neither has a default value. The skill does not address what to do when the user does not provide one or both IDs.
- Workflow lacks numbered steps and explicit input/output definitions per step.
- No checkpoint for confirming the correct document is being queried when IDs are inferred from context.
- Triggers fire on history keywords but the skill does not explain how to resolve docId from implicit context (e.g., "this file" in a conversation).
- `See [references/](references/)` is generic; references directory may not be populated.

## Dimension 8 dry-run notes

**Test 1: "show version history of document 123"**
- Skill has vid=1 hardcoded in examples but the prompt does not specify the repository. The skill would need to either use a default VID or prompt for it.
- The prompt gives docId=123 but no vid. All examples show `docsys doc history 1 123` with vid=1 assumed. This is ambiguous — the agent might guess wrong.
- Score: 5/10 — docId extracted correctly, but VID sourcing is unclear.

**Test 2: "这个文件什么时候被修改过" (Chinese: "when was this file modified")**
- Skill triggers on "修改" in triggers list. However, the prompt provides no docId and no vid at all. The skill does not guide how to resolve the implicit "this file" from conversation context.
- Without context resolution guidance, the agent cannot produce the correct call.
- Score: 3/10 — trigger works, but no parameter extraction or context-resolution guidance.

## Suggested priority improvements (P0-P3)

1. P0 (Actual performance — implicit context): Add guidance for resolving "this file"/"this document" from conversation context. Example: if the user says "show history of this file" without IDs, the skill should call `docsys repos` (to find the repo) then `docsys doc list` (to find the doc) before calling history.
2. P0 (Checkpoint design): Add a note that when IDs are inferred from context rather than explicitly provided, the skill should display the resolved docId/name for user confirmation before calling history.
3. P1 (Instruction specificity): Clarify the vid default or indicate that vid must always be provided. Add a fallback path: if vid is missing, prompt user; if docId is missing, try resolving from current folder context.
4. P2 (Workflow clarity): Number workflow steps with input/output per step. E.g., Step 1: Input = user prompt, Output = extracted vid + docId.
5. P3 (Resource integration): Populate `references/` with actual links to list_repos and doc_list skills.
