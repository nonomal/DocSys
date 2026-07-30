---
name: rag_chat
description: Chat with AI using relevant documents as context (RAG)
category: ai
version: 1.1.0
author: DocSys Team
permissions:
  - read
tags: [ai, rag, retrieval, document-qa, context, knowledge-base]
---

# RAG Chat

Chat with AI using relevant documents as context (Retrieval-Augmented Generation). Ideal for questions about specific documents or repositories.

## Triggers

RAG chat, chat with docs, 基于文档问答, 文档问答, 文档对话, rag, 问文档, ask about docs, question about document, 文档智能问答, 读取文档内容回答, RAG 问答, summarize doc, compare documents

## Workflow

1. **Parse query**: Extract question from user input. Preserve original language.
2. **Select model**: If user specifies a model → validate it. For analytical/comparative questions → recommend larger model if available.
3. **Call CLI**: `docsys rag <query> [model]`
4. **⚠️ Answer quality checkpoint**: Inspect the AI answer before presenting to user.
   - **If citations present**: Present answer with cited document IDs `[DocID]`. ✓ Safe to show.
   - **If no citations**: Wrap in ⚠️ warning: `"⚠️ No document citations found. Answer may be based on general knowledge or hallucination. Verify key claims against source documents before relying on this answer."`
   - **If answer is vague/contradictory**: Add `"⚠️ Answer confidence is low. Consider rephrasing the question or searching with different keywords."`
5. **No-results fallback**: If "No relevant documents found" → suggest broadening query or falling back to `docsys search <keywords>`.
6. **Format output**: Present answer with source citations in `[DocID]` format. Never omit citations warning.

## CLI Command

```bash
docsys rag <query> [model]
docsys ask <query> [model]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| query | string | yes | Question about documents |
| model | string | no | AI model to use (default: configured model) |

## Examples

### Example 1
User: "what does the Q4 report say about revenue?"
Skill triggers → calls: `docsys rag what does the Q4 report say about revenue?`
Output: AI answer with document citations

### Example 2
User: "这份文档的主要观点是什么"
Skill triggers → calls: `docsys rag 这份文档的主要观点是什么`
Output: 基于文档内容回答

### Example 3
User: "summarize the key points from the architecture doc"
Skill triggers → calls: `docsys rag summarize the key points from the architecture doc`
Output: AI response with citations

### Example 4 (vid-filtered)
User: "what is the deployment policy in repo 2"
Skill triggers → Step 1: query="deployment policy" → Step 2: vid=2 → calls: `docsys rag deployment policy 2`
Output: AI answer with citations from documents in repo 2

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| answer | string | AI response text |
| citations | string[] | Array of cited DocIDs, e.g. `[doc_123]` |
| model | string | Model used (if specified) |
| lang | string | Response language matches input (zh/en) |

**Success with citations (EN):** `{answer}\n📎 Sources: {citations.join(", ")}`
**Success with citations (中文):** `{answer}\n📎 来源：{citations.join(", ")}`
**⚠️ No citations (EN):** `⚠️ No document citations found. Answer may be based on general knowledge or hallucination. Verify key claims against source documents before relying on this answer.`
**⚠️ No citations (中文):** `⚠️ 未找到文档引用。回答可能基于通用知识或虚构内容。请核实关键结论。`
**⚠️ Low confidence (EN):** `⚠️ Answer confidence is low. Consider rephrasing the question or using different keywords.`
**⚠️ Low confidence (中文):** `⚠️ 回答置信度较低。请尝试换一种表述方式或使用不同关键词。`
**No documents found (EN):** `⚠️ No relevant documents found in repo {vid}. Try: broadening keywords, removing filters, or run 'docsys search <query> {vid}' first.`
**No documents found (中文):** `⚠️ 在仓库 {vid} 中未找到相关文档。请尝试：扩大关键词范围、移除过滤器，或先运行 'docsys search <关键词> {vid}' 进行搜索。`
**Error (EN):** `❌ {error_message}`
**Error (中文):** `❌ {错误信息}`

## Error Handling

| Error | Cause |
|-------|-------|
| Query is required | Empty query |
| No relevant documents found | Query doesn't match docs |
| AI service unavailable | LLM service down |
| Model not found | Invalid model |

See [references/](references/) for related skills and API docs.
