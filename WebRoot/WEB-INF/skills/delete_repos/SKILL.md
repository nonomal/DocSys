---
name: delete_repos
description: Delete a repository from DocSystem
category: repository
version: 1.1.0
author: DocSys Team
permissions:
  - admin:repository
tags: [repository, delete, remove, destructive]
---

# Delete Repository

Permanently delete a repository and all its documents. Requires admin permission.

## Triggers

Delete repo, delete repository, remove repo, 删除仓库, 移除仓库, 删除repo, 删库, destroy repo, erase repository, repo 删除

## Workflow

1. **Extract vid**: Parse repository ID from user input.
2. **Resolve name→vid**: If user gives a repository name (e.g., "Archive_2024") → call `docsys repos list` to map name to vid. If ambiguous → ask user to confirm.
3. **⚠️⚠️ MANDATORY CONFIRM — DESTRUCTIVE**: Show repository name, vid, and document count → ask user to type "CONFIRM" to proceed. **This action is irreversible.** If user declines or does not type CONFIRM → stop.
4. **Call CLI**: `docsys repos delete <vid>`
5. **Present result**: Confirm deletion is complete. No rollback available.

## CLI Command

```bash
docsys repos delete <vid>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID to delete |

## Examples

### Example 1
User: "delete repository with id 5"
Skill triggers → calls: `docsys repos delete 5`
Output: Confirms deletion; returns result

### Example 2
User: "删除仓库 3"
Skill triggers → calls: `docsys repos delete 3`
Output: 删除请求结果

### Example 3 — Name-based deletion with full resolution
User: "remove the Archive_2024 repo"
Skill triggers → Step 1: user gave name "Archive_2024", not numeric vid
→ Step 2: `docsys repos list` → find repo named "Archive_2024" → resolve vid=7
→ Step 3: ⚠️⚠️ "You are about to PERMANENTLY DELETE repository 'Archive_2024' (vid=7). This will delete ALL documents. Type CONFIRM to proceed." → user types "CONFIRM"
→ Step 4: `docsys repos delete 7`
→ Step 5: `✅ Repository Archive_2024 (vid=7) deleted. No rollback available.`
Output: Deletion confirmed — irreversible

## Output Format

**Success:** `✅ Repository '{name}' (vid={vid}) deleted. No rollback available.`
**Warning:** `⚠️ Destructive operation. This will permanently remove all documents.`
Error: "Repository not found" | "Permission denied" | "Usage: repos delete <id>"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: repos delete <id> | Missing vid parameter |
| Repository not found | Invalid vid |
| Permission denied | Requires admin role |
| Deletion failed | API error |

See [references/](references/) for related skills and API docs.
