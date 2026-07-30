# banner — Baseline Evaluation

## Scores (dimension: raw × weight → points / max)

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 5/10 | 1.5 | 7.5/15 |
| 3 | Boundary conditions | 6/10 | 1.0 | 6.0/10 |
| 4 | Checkpoint design | 5/10 | 0.7 | 3.5/7 |
| 5 | Instruction specificity | 5/10 | 1.5 | 7.5/15 |
| 6 | Resource integration | 6/10 | 0.5 | 3.0/5 |
| 7 | Overall architecture | 7/10 | 1.5 | 10.5/15 |
| 8 | Actual performance | 9/10 | 2.5 | 22.5/25 |
| **TOTAL** | | | | **66.9/100** |

## Key strengths
- Frontmatter complete: name规范, description含"Display"触发词, all required fields
- Extremely simple, low-stakes operation — low risk of harm
- Bilingual triggers (English + Chinese)
- Single optional parameter — minimal surface area for errors
- Actual performance excellent: both test prompts are straightforward

## Key weaknesses
- Workflow is essentially absent — no numbered steps, no input/output per step definition
- Only 1 error case covered ("Banner not found") — no coverage of what happens if name is empty string vs omitted, or if banner file is corrupted
- Instruction specificity is minimal — no guidance on what "banner content" looks like (plain text? ASCII art? HTML?)
- No clarification on what the "default banner" is or how it is selected
- references/ section not verified

## Dimension 8 dry-run notes

**Prompt 1:** "show the welcome banner"
Skill identifies this as default banner case. Calls `docsys banner` (no name argument). Expected: default welcome banner displayed. Skill correctly maps this. Score: 9/10.

**Prompt 2:** "display the holiday banner"
Skill extracts name='holiday'. Calls `docsys banner holiday`. Expected: holiday banner content or "Banner not found" if missing. Skill handles both paths correctly via the error table. Score: 9/10.

## Suggested priority improvements (P0-P3)
1. P1: Workflow clarity — add at least 2 numbered steps (1. Parse optional name, 2. Call CLI, 3. Return banner content or error)
2. P2: Boundary conditions — add "empty string name" vs "omitted name" distinction; add guidance on corrupted banner file
3. P2: Instruction specificity — describe what the banner output format looks like (ASCII art, plain text, etc.)
4. P3: Resource integration — verify references/ directory content
