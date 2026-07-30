---
name: backup_repo
description: Start a repository backup and check its status
category: repository
version: 1.1.0
author: DocSys Team
permissions:
  - admin:repository
tags: [repository, backup, restore, admin]
---

# Backup Repository

> **Note:** This is an alias of the `backup_repos` skill. All functionality
> is identical; use whichever skill name fits your workflow.

Start a repository backup to a specified path or default location. Also check backup task status.

## Triggers

Backup repo, backup repository, 备份仓库, 仓库备份, 备份, 开始备份, make a backup, back up, repository backup, backup my repo, 备份还原, 备份仓库 1

## CLI Command

```bash
docsys backup <vid> [path]
docsys backup-status <task-id>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID to backup |
| path | string | no | Backup destination path |
| task-id | string | yes (for status) | Backup task ID returned from backup command |

## Examples

### Example 1
User: "backup repository 1 to F:/backups"
Skill triggers -> calls: `docsys backup 1 F:/backups`
Output: Backup started, returns task-id

### Example 2
User: "备份仓库 2"
Skill triggers -> calls: `docsys backup 2`
Output: 备份开始，使用默认路径

### Example 3
User: "check backup status for task backup-2025-12-15-001"
Skill triggers -> calls: `docsys backup-status backup-2025-12-15-001`
Output: Backup progress and status

## Output Format

Success (backup): "Backup started" with task-id.
Success (status): Progress, size processed, estimated completion.
Error: "Repository not found", "Permission denied", "Backup already running"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: backup <vid> [path] | Missing vid |
| Repository not found | Invalid vid |
| Permission denied | Requires admin role |
| Backup already running | Concurrent backup conflict |
| Task not found | Invalid task-id for status |
