---
name: download_doc
description: Download a document to local filesystem
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - read:document
tags: [document, download, file-transfer, export, get]
---

# Download Document

Download a document from a repository to the local filesystem.

## Triggers

Download, 下载, download doc, download file, 下载文档, 下载文件, export file, get file, 下载这个文件, save to local, retrieve document, 下载到本地

## Workflow

1. **Extract parameters**: Parse vid and docId from user input.
2. **Resolve IDs if missing**: If vid missing → call `docsys repos list` to find available repos. If docId missing but user says "this document" → call `docsys docs list <vid>` to locate it. If still unresolvable → ask user directly. Do not guess IDs.
3. **Check local path**: If user specifies a local path → use it. Otherwise → save to current working directory with the document's original filename.
4. **⚠️ Warn on overwrite**: If target file already exists locally → warn user before overwriting.
5. **Call CLI**: `docsys download <vid> <docId>`
6. **Present result**: Show saved file path and size.

## CLI Command

```bash
docsys download <vid> <docId>
docsys get <vid> <docId>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID |

## Examples

### Example 1
User: "download document 123 from repo 1"
Skill triggers → calls: `docsys download 1 123`
Output: Download confirmation with saved path

### Example 2
User: "下载文档 456 from repo 1"
Skill triggers → Step 1: vid=1, docId=456 → Step 4: calls: `docsys download 1 456`
Output: 下载结果

### Example 3
User: "save file 789 to local"
Skill triggers → Step 1: docId=789, vid=missing → Step 2: calls `docsys repos list` to find available repos → user specifies vid=2 → Step 4: calls: `docsys download 2 789`
Output: File saved to local disk

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| filename | string | Original document filename |
| local_path | string | Absolute path where file was saved |
| size | string | File size (bytes/KB/MB) |
| vid | number | Repository ID |
| docId | number | Document ID |

**Success (EN):** `✅ Downloaded | {filename} ({size}) → {local_path}`
**Success (中文):** `✅ 已下载 | {filename} ({size}) → {local_path}`
**Overwrite warning:** `⚠️ File already exists at {local_path}. Overwrite? [y/N]`
Error: "Document not found" | "Permission denied" | "Disk full — file not written"

## Error Handling

| Error | Cause |
|-------|-------|
| Usage: download <vid> <docId> | Missing arguments |
| Repository not found | Invalid vid |
| Document not found | Invalid docId |
| Permission denied | No read access |
| File not writable | Disk full or path permissions |

See [references/](references/) for related skills and API docs.
