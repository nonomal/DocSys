---
name: upload_doc
description: Upload a local file to a repository
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - write:document
tags: [document, upload, file-transfer, import, put]
---

# Upload Document

Upload a local file to a repository.

## Triggers

Upload, 上传, upload doc, upload file, 上传文档, 上传文件, put file, import file, 上传这个文件, add file to repo, send file, 上传文件

## Workflow

1. **Extract parameters**: Parse file path, vid, and optional pid from user input.
2. **Resolve IDs if missing**: If vid missing → call `docsys repos list`. If pid is a folder name (e.g., "reports folder") → call `docsys docs list <vid>` to find folder ID.
3. **Validate file**: Check local file exists. If not found → report error before calling CLI.
4. **⚠️ Confirm before uploading**: Show file name, target repo and folder → warn "Will create or overwrite existing file with same name." Ask "Upload [filename]?" (confirm). If user declines → stop.
5. **Call CLI**: `docsys upload <file> <vid> [pid]`
6. **Present result**: Show new document ID and target location.

## CLI Command

```bash
docsys upload <file> <vid> [pid]
docsys put <file> <vid> [pid]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| file | string | yes | Local file path to upload |
| vid | number | yes | Repository ID |
| pid | number | no | Target folder ID (default: 0 = root) |

## Examples

### Example 1
User: "upload F:/report.pdf to repo 1"
Skill triggers → calls: `docsys upload F:/report.pdf 1`
Output: Upload confirmation with new document ID

### Example 2
User: "上传文件 data.xlsx 到仓库 2 的文件夹 5"
Skill triggers → calls: `docsys upload data.xlsx 2 5`
Output: 上传结果

### Example 3
User: "put report.docx into the reports folder"
Skill triggers → Step 1: file="report.docx", vid and pid missing → Step 2: calls `docsys repos list` to find available repos → user specifies vid=3 → calls `docsys docs list 3` to find "reports" folder → pid=12 → Step 4: confirm "Upload report.docx to repo 3, folder 'reports'?" → Step 5: calls: `docsys put report.docx 3 12`
Output: Upload result with document ID

## Output Format

Success: Document uploaded successfully. ID: <docId>, Location: repo <vid>/folder <pid>, Size: <size>
Error: "File not found" (local file doesn't exist — check path), "Repository not found" (invalid vid — run `docsys repos list`), "Permission denied" (no write access), "File too large" (exceeds max upload size — see `docsys system-config`)

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: upload <file> <vid> | Missing arguments |
| File not found | Invalid file path |
| File too large | Exceeds max upload size |
| Repository not found | Invalid vid |
| Permission denied | No write access |

See [references/](references/) for related skills and API docs.
