# add_doc — Baseline Evaluation

## Scores (dimension: raw × weight → points / max)

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 7/10 | 1.5 | 10.5/15 |
| 3 | Boundary conditions | 8/10 | 1.0 | 8.0/10 |
| 4 | Checkpoint design | 5/10 | 0.7 | 3.5/7 |
| 5 | Instruction specificity | 8/10 | 1.5 | 12.0/15 |
| 6 | Resource integration | 6/10 | 0.5 | 3.0/5 |
| 7 | Overall architecture | 8/10 | 1.5 | 12.0/15 |
| 8 | Actual performance | 7/10 | 2.5 | 17.5/25 |
| **TOTAL** | | | | **72.9/100** |

## Key strengths
- Frontmatter is complete: name规范, description含"Create"触发词, all required fields present, well under 1024 chars
- Error handling table is thorough: 6 error cases covering missing args, empty name, invalid chars, name conflict, permissions, invalid repo
- Parameter specificity is good: explicit max 255 char limit and forbidden character list (`/\:*?"<>|`)
- CLI command aliases (doc add / mkdir) clearly shown
- Triggers section is comprehensive (English + Chinese)

## Key weaknesses
- Workflow lacks numbered steps with explicit input/output per step — content flows but no sequential structure
- No confirmation checkpoint before executing a write/destructive operation (creating a folder)
- No guidance on what to do when vid is missing beyond "prompt for missing repo ID" — not explicit enough
- references/ section not verified to contain actual content

## Dimension 8 dry-run notes

**Prompt 1:** "create a folder called reports in repo 1"
Skill correctly extracts vid=1, name='reports'. Calls `docsys doc add 1 reports`. Expected outcome matches. Score: 8/10.

**Prompt 2:** "mkdir myproject"
vid is missing; the example shows `docsys mkdir <vid> myproject` with a placeholder. The skill needs to recognize missing vid and prompt the user, not substitute a literal placeholder. Score: 6/10 — good that the error table covers "Usage: doc add <vid> <name>", but the example itself models bad behavior.

**Prompt 3:** "新建文件夹 Q4报告 在仓库 1 中"
Correctly extracts vid=1 and name='Q4报告', calls `docsys mkdir 1 Q4报告`. Chinese output confirmed. Score: 8/10.

## Suggested priority improvements (P0-P3)
1. P0: Checkpoint design — add explicit user confirmation step before executing write operations (e.g., "You are about to create folder '<name>' in repo <vid>. Confirm?")
2. P1: Workflow clarity — add numbered steps (1. Parse parameters, 2. Validate inputs, 3. Execute CLI command, 4. Return result) with clear input/output per step
3. P2: Instruction specificity — clarify what "prompt for missing repo ID" looks like (exact wording or example of the prompt)
4. P3: Resource integration — verify references/ directory contains actual content and document any helper scripts
