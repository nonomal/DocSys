# backup_repos — Baseline Evaluation

## Scores (dimension: raw × weight → points / max)

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 7/10 | 1.5 | 10.5/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 5/10 | 0.7 | 3.5/7 |
| 5 | Instruction specificity | 7/10 | 1.5 | 10.5/15 |
| 6 | Resource integration | 6/10 | 0.5 | 3.0/5 |
| 7 | Overall architecture | 8/10 | 1.5 | 12.0/15 |
| 8 | Actual performance | 8/10 | 2.5 | 20.0/25 |
| **TOTAL** | | | | **72.9/100** |

## Key strengths
- Frontmatter complete: name规范, description含"Start"和"check"触发词, all required fields
- Handles two distinct operations (backup + status check) with separate CLI commands shown
- Error handling covers concurrent backup conflict ("Backup already running") — a thoughtful edge case
- task-id parameter clearly required only for status sub-command
- Triggers comprehensive and bilingual

## Key weaknesses
- No numbered workflow steps with input/output per step
- No confirmation checkpoint before initiating a potentially large I/O operation (backup)
- No guidance on backup size limits, what "default path" means, or how to find the task-id if forgotten
- No explicit step for validating the vid before starting backup
- references/ section not verified to contain actual content

## Dimension 8 dry-run notes

**Prompt 1:** "backup repository 1 to F:/backups"
Skill correctly extracts vid=1 and path='F:/backups'. Calls `docsys backup 1 F:/backups`. Expected: task-id returned, confirmation that backup initiated. Score: 8/10.

**Prompt 2:** "check backup status for task backup-2025-12-15-001"
Skill correctly identifies this as a status check, extracts task-id. Calls `docsys backup-status backup-2025-12-15-001`. Expected: progress, size, estimated completion. Skill covers this. Score: 8/10.

**Prompt 3:** "备份仓库 2"
Skill extracts vid=2, no path provided. Uses default backup path. Calls `docsys backup 2`. Expected: Chinese confirmation, task-id returned. Score: 8/10.

## Suggested priority improvements (P0-P3)
1. P0: Checkpoint design — add confirmation step before starting a backup (warn about I/O cost, expected size, destination)
2. P1: Workflow clarity — add numbered steps with input/output per step; clarify what the "default path" is
3. P2: Boundary conditions — add guidance on what to do if task-id is unknown (list running backups)
4. P3: Resource integration — verify references/ directory content
