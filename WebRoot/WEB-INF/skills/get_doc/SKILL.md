---
name: get_doc
description: Get document details or preview content
category: document
version: 1.1.0
author: DocSys Team
permissions:
  - read:document
tags: [document, get, view, metadata, preview, cat]
---

# Get Document

Retrieve document details, metadata, or preview content. For text files shows content; binary files show metadata only.

## Triggers

Get doc, view doc, cat doc, 查看文档, 获取文档, 文档详情, 查看文档内容, show document, view this file, what's in this doc, 文档信息, 文件内容查看

## Workflow

1. **Extract parameters**: Parse vid and docId from user input.
2. **Resolve IDs if missing**: If vid missing → call `docsys repos list` to resolve. If docId missing but user says "this doc" → call `docsys docs list <vid>` to locate it.
3. **Large file guard**: If file >1MB or estimated >500 lines → offer preview first: "File is large. Show preview (first 100 lines)?" If user confirms → show full content.
4. **Call CLI**: `docsys doc get <vid> <docId>` or `docsys cat <vid> <docId>`
5. **Format output**: Text files (.txt, .md, .json, .xml, .java, .py, .csv) → show content. Binary files → show metadata only.

## CLI Command

```bash
docsys doc get <vid> <docId>
docsys cat <vid> <docId>
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| vid | number | yes | Repository ID |
| docId | number | yes | Document ID |

## Examples

### Example 1
User: "show details of document 123 in repo 1"
Skill triggers → calls: `docsys doc get 1 123`
Output: Document metadata (and content for text files)

### Example 2
User: "cat doc 456"
Skill triggers → Step 1: docId=456, vid=missing → Step 2: calls `docsys repos list` → user specifies vid=2 → calls: `docsys cat 2 456`
Output: Document details or content

### Example 3
User: "查看文档 789 的内容"
Skill triggers → Step 1: docId=789, vid=missing → Step 2: calls `docsys repos list` → user specifies vid=3 → Step 3: (file size unknown yet) → calls: `docsys doc get 3 789` → if large → offer preview → calls: `docsys cat 3 789`
Output: 文档详情或内容预览

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| name | string | Document name |
| docId | number | Document ID |
| vid | number | Repository ID |
| size | string | File size (bytes/KB/MB) |
| type | string | MIME type (text/binary) |
| updated | string | Last modified date |
| content | string | Full content (text files only) |

**Text file (EN):** `✅ {name} ({size}) | Last modified: {updated}\n{content preview}`
**Text file (中文):** `✅ {name}（{size}）| 修改时间: {updated}\n{内容预览}`
**Binary file (EN):** `✅ {name} ({size}) | Type: {type} | Updated: {updated}\n⚠️ Binary — metadata only, content not displayed.`
**Binary file (中文):** `✅ {name}（{size}）| 类型: {type} | 修改时间: {updated}\n⚠️ 二进制文件 — 仅显示元数据，内容不可预览。`
**Large file (EN):** `⚠️ File is large ({size}). Show preview (first 100 lines)? [y/N]`
**Large file (中文):** `⚠️ 文件较大（{size}）。显示预览（前 100 行）？[y/N]`
**Not found (EN):** `❌ Document {docId} not found in repo {vid}. Verify the docId and try again.`
**Not found (中文):** `❌ 文档 {docId} 在仓库 {vid} 中不存在。请检查 docId 是否正确。`
**Permission denied (EN):** `❌ Permission denied. You don't have read access to this document.`
**Permission denied (中文):** `❌ 权限不足。您没有此文档的读取权限。`
Error: "Document not found" | "Permission denied" | "Usage: doc get <vid> <docId>" |

See [references/](references/) for related skills and API docs.
