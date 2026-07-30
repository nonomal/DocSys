# download_doc — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 7/10 | 0.8 | 5.6/8 |
| 2 | Workflow clarity | 6/10 | 1.5 | 9.0/15 |
| 3 | Boundary conditions | 8/10 | 1.0 | 8.0/10 |
| 4 | Checkpoint design | 3/10 | 0.7 | 2.1/7 |
| 5 | Instruction specificity | 8/10 | 1.5 | 12.0/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 10/10 | 1.5 | 15.0/15 |
| 8 | Actual performance | 15/10 | 2.5 | 17.5/25 |
| **TOTAL** | | | | **69.7/100** |

## Key strengths
- Frontmatter is complete and well-structured.
- Excellent trigger coverage with bilingual support across multiple phrasings.
- Error-handling table is the strongest of the evaluated skills: covers missing args, invalid vid, invalid docId, permission, AND "File not writable" (disk full or path permissions).
- Output format specifies the expected confirmation format (saved path + size).
- Overall architecture is clean with no redundancy.

## Key weaknesses
- Workflow steps are not numbered and do not define explicit input/output per step.
- No user confirmation checkpoint before downloading (minor risk since downloads are non-destructive, but overwriting local files is possible).
- When vid is not provided in the prompt (Example 2), the skill does not guide how to resolve it (must list repos first).
- Duplicate CLI command block (`docsys download` and `docsys get` are listed as separate commands; clarify whether they are aliases or serve different purposes).
- `See [references/](references/)` is generic with no actual links or evidence the directory is populated.

## Dimension 8 dry-run notes

**Test 1: "download document 123 from repo 1"**
- Skill extracts vid=1 and docId=123 cleanly. Calls `docsys download 1 123`.
- Output format is well-specified: confirmation with saved path and size.
- Score: 8/10 — full parameter extraction and correct command invocation.

**Test 2: "把这个文档下载到本地" (Chinese: "download this document to local")**
- Skill triggers on Chinese "下载" keywords. However, neither vid nor docId is provided in the prompt. The skill does not guide how to resolve the implicit "this document" from conversation context.
- The expected behavior says "attempts to resolve docId from current context or recent conversation, returns saved path or prompts for clarification" — but the skill itself provides no such guidance.
- Score: 3/10 — trigger works, but parameter resolution from implicit context is not guided in the skill text.

## Suggested priority improvements (P0-P3)

1. P0 (Actual performance — implicit context resolution): Add a sub-step for resolving the target document when no explicit IDs are provided. Guide the agent to first call `docsys repos` (if vid unknown) or `docsys doc list` (if docId unknown). If neither is resolvable from context, the skill should explicitly prompt the user for clarification rather than guessing.
2. P1 (Workflow clarity): Number the workflow steps and define input/output for each step. E.g., Step 1: Input = user prompt, Output = {vid, docId, local path}. Step 2: Execute download. Step 3: Report result.
3. P1 (Instruction specificity): Clarify what "File not writable" means in practice and suggest fallback actions (e.g., try alternative path, check disk space).
4. P2 (Checkpoint design): Add a lightweight confirmation step: if the file already exists locally, warn the user before overwriting.
5. P3 (Resource integration): Populate `references/` with actual links to list_repos and doc_list skills.
