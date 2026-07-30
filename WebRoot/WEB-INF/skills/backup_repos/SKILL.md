---
name: backup_repos
description: Start a repository backup and check its status
category: repository
version: 1.1.0
author: DocSys Team
permissions:
  - admin:repository
tags: [repository, backup, restore, admin]
---

# Backup Repository

Start a repository backup to a specified path or default location. Also check backup task status.

## Triggers

Backup repo, backup repository, 备份仓库, 仓库备份, 备份, 开始备份, make a backup, back up, repository backup, backup my repo, 备份还原, 备份仓库 1, check backup status

## Workflow

### Mode 1: Start backup
1. **Extract vid and path**: Parse from user input.
2. **Resolve vid if missing**: If vid not provided → call `docsys repos list` to resolve.
3. **⚠️⚠️ Confirm before backup**: Show repo name, vid, estimated size, destination path. Ask user to **type exactly `backup`** (not just "yes" or "confirm") to start. If user types anything else or says no → stop and report "Backup cancelled."
4. **Call CLI**: `docsys backup <vid> [path]`
5. **Present result**: Show task-id. Note "Use backup-status [task-id] to monitor progress."

### Mode 2: Check backup status
1. **Extract task-id**: From user input. If unknown → call `docsys backup-status` with no args to list running backups.
2. **Call CLI**: `docsys backup-status <task-id>`
3. **Present result**: Show progress %, size processed, estimated completion.

## CLI Command

```bash
docsys backup <vid> [path]
docsys backup-status <task-id>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID to backup |
| path | string | no | Backup destination path (default: system default) |
| task-id | string | yes (for status) | Backup task ID returned from backup command |

## Examples

### Example 1
User: "backup repository 1 to F:/backups"
Skill triggers → calls: `docsys backup 1 F:/backups`
Output: Backup started, returns task-id

### Example 2
User: "备份仓库 2"
Skill triggers → Step 2: vid=2 → Step 3: confirm → calls: `docsys backup 2`
Output: 备份开始，使用默认路径

### Example 3
User: "backup the project repo"
Skill triggers → Step 1: name="project repo", vid=missing → Step 2: calls `docsys repos list` to find matching repo → vid=5 → Step 3: ⚠️⚠️ confirm → user types "backup" → calls: `docsys backup 5`
Output: ✅ Backup started | task-id: backup-2025-12-15-001 | vid: 5 | → /default/backup/path

### Example 4
User: "check backup status for task backup-2025-12-15-001"
Skill triggers → calls: `docsys backup-status backup-2025-12-15-001`
Output: Backup progress and status

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| task_id | string | Backup task identifier |
| vid | number | Repository ID backed up |
| dest_path | string | Backup destination path |
| progress | string | Completion percentage (for status) |
| size | string | Data size processed |

**Success (backup):** `✅ Backup started | task-id: {task_id} | vid: {vid} | → {dest_path}`
**Success (status):** `✅ Backup {task_id} | {progress}% | {size} processed`
> ZH: `✅ 备份已开始 | 任务ID: {task_id} | 仓库ID: {vid} | 目标路径: {dest_path}`
Error: "Repository not found" | "Permission denied" | "Backup already running" | "Task not found"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: backup <vid> [path] | Missing vid |
| Repository not found | Invalid vid |
| Permission denied | Requires admin role |
| Backup already running | Concurrent backup conflict |
| Task not found | Invalid task-id for status |

See [references/](references/) for related skills and API docs.
