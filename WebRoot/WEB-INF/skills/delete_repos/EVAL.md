# delete_repos — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 7/10 | 0.8 | 5.6/8 |
| 2 | Workflow clarity | 6/10 | 1.5 | 9.0/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 2/10 | 0.7 | 1.4/7 |
| 5 | Instruction specificity | 8/10 | 1.5 | 12.0/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 10/10 | 1.5 | 15.0/15 |
| 8 | Actual performance | 15/10 | 2.5 | 17.5/25 |
| **TOTAL** | | | | **69.0/100** |

## Key strengths
- Frontmatter is complete with all required fields, valid YAML, and a clear destructive-operation warning in the description.
- Trigger list is extensive and bilingual (EN + ZH), covering both direct and colloquial phrasing.
- Comprehensive error-handling table covering missing args, invalid VID, permission, and API failure.
- Overall architecture is clean and logical; no redundancy or missing sections.

## Key weaknesses
- No user confirmation checkpoint before executing a destructive delete — the most dangerous omission. The description mentions "admin permission" but the skill never prompts the user to confirm before running `docsys repos delete`.
- Workflow steps are not numbered and do not define explicit input/output per step.
- The Example 3 uses a vague repository name ("Archive_2024") that requires a name-to-VID lookup not described anywhere in the skill.
- `See [references/](references/)` is present but the references directory is not populated or verified.
- VID format validation and confirmation are absent (e.g., what if user says "delete repo 5" but meant "delete repo 50"?).

## Dimension 8 dry-run notes

**Test 1: "delete repository with id 5"**
- Skill extracts vid=5, calls `docsys repos delete 5`. Execution path is correct and well-guided.
- Score: 8/10 — straightforward; VID extracted cleanly.

**Test 2: "remove the Archive_2024 repo"**
- Skill uses name "Archive_2024" but only accepts numeric VID. No explicit guidance on resolving a name to VID (requires a prior `docsys repos` call first).
- Without a prior list_repos call, the agent cannot determine the VID. The skill does not guide this lookup step.
- Score: 4/10 — gap between what the test expects and what the skill guides.

## Suggested priority improvements (P0-P3)

1. P0 (Checkpoint design — score 2/10): Add a mandatory user confirmation step before executing `docsys repos delete`. Example: "You are about to permanently delete repository [name/VID]. Type CONFIRM to proceed." This is a destructive operation with no undo.
2. P1 (Actual performance — ambiguous VID/name): Add a "VID Resolution" sub-step: if the user provides a repository name instead of a numeric ID, the skill must first call `docsys repos` to list repos, then map the name to VID, then confirm before deleting.
3. P2 (Workflow clarity): Number the workflow steps explicitly (1. Parse parameters, 2. Resolve names to IDs if needed, 3. Confirm with user, 4. Execute command, 5. Return result). Define input/output for each step.
4. P3 (Resource integration): Populate the `references/` directory with actual API docs and linked skills (e.g., list_repos).
