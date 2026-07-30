---
name: system_help
description: Show help and available commands
category: system
version: 1.1.0
author: DocSys Team
permissions:
  - read
tags: [help, usage, commands, guide]
---

# Help

Display all available commands and usage help.

## Triggers

Help, 帮助, ?, 使用说明, commands, 命令帮助, 怎么用, usage, how to use, show help, command help, 使用帮助, 求助

## Workflow

1. **Identify scope**: If user names a specific command → extract it. If not → show all commands.
2. **Valid subcommands**: doc, repos, search, upload, download, lock, unlock, login, logout, whoami, status, config, models, backup, help, browser-use
3. **⚠️ Scoped repos help**: If user asks about repository commands (e.g. "repos help", "what can I do with repos") → show repo-specific commands: `backup_repos`, `create_repos`, `delete_repos`, `list_repos`, `repos_info`, `search_in_repo`
4. **Call CLI**: `docsys help [command]` or `docsys ? [command]`
5. **Format output**: Present command list with name and brief description per command. For specific command: show syntax, description, and examples.

## CLI Command

```bash
docsys help [command]
docsys ? [command]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| command | string | no | Specific command to get detailed help |

## Examples

### Example 1
User: "show help"
Skill triggers → calls: `docsys help`
Output: All available commands

### Example 2
User: "help on the upload command"
Skill triggers → calls: `docsys help upload`
Output: Upload command usage details

### Example 3
User: "how do I search for documents"
Skill triggers → calls: `docsys help search`
Output: Search command help

### Example 4 (scoped repos help)
User: "what can I do with repositories"
Skill triggers → Step 3: repos scope detected → shows repo-specific commands: backup, create, delete, list, repos-info, search-in-repo → calls: `docsys help` (fallback to all if needed)
Output: 可用仓库操作命令：备份仓库、创建仓库、删除仓库、列出仓库、仓库详情、仓库内搜索

## Output Format

Success: Help text with command list or specific command details.
Error: "Unknown command: xxx"

## Error Handling

| Error | Cause |
|-------|-------|
| Unknown command | Command not found |
| Incomplete arguments | Missing required args |

See [references/](references/) for related skills and API docs.
