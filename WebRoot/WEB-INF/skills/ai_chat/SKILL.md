---
name: ai_chat
description: Chat with AI assistant for general questions and conversations
category: ai
version: 1.1.0
author: DocSys Team
permissions:
  - read
tags: [ai, chat, conversation, assistant, llm]
---

# AI Chat

Chat with the built-in AI assistant for general questions and conversations.

## Triggers

Chat, ai chat, ai, 聊天, 问答, 问问题, AI对话, 智能问答, ask AI, talk to AI, 和 AI 聊天, 人工智能对话, 帮我问问 AI, 解释一下, write code

## Workflow

1. **Parse message**: Extract the user's question or request. Preserve original language (Chinese/English).
2. **Select model**: If user specifies a model → validate it against `docsys models list`. If invalid → return "Model not found". If not specified → use default configured model.
3. **Call CLI**: `docsys chat <message> [model]`
4. **Timeout handling**: Default timeout = 30s. If no response → retry once (max 1 retry).
5. **Format response**: Pass AI response through as-is. If code: wrap in markdown code blocks. If harmful content detected → return refusal message.
6. **Error recovery**: If "AI service unavailable" → wait 5s and retry once. If still unavailable → return "AI service temporarily unavailable, please try again later."

## CLI Command

```bash
docsys chat <message> [model]
docsys ai <message> [model]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| message | string | yes | Question or message to send |
| model | string | no | AI model to use (default: configured model) |

## Examples

### Example 1
User: "what is DocSystem?"
Skill triggers → calls: `docsys chat what is DocSystem?`
Output: AI response

### Example 2
User: "帮我解释这个项目的架构"
Skill triggers → calls: `docsys chat 帮我解释这个项目的架构`
Output: AI 回复

### Example 3 — Code request with retry
User: "write a Python quicksort"
Skill triggers → Step 1: message="write a Python quicksort"
→ Step 2: model not specified → use default
→ Step 3: `docsys chat write a Python quicksort`
→ Step 4: response received within 30s (or retry once)
→ Step 5: detected code in response → wrap in ```python code blocks ```
→ Output: AI response with formatted code

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| content | string | Raw AI response text |
| model | string | Model used (if specified) |
| lang | string | Response language matches input (zh/en) |
| has_code | bool | True if response contains code blocks |

**Success (EN):** `{content}\n_model: {model}`
**Success (中文):** `{content}\n_模型: {model}`
**Code output (EN):** `\`\`\`\n{code}\n\`\`\``
**Code output (中文):** `以下是代码示例：\n\`\`\`\n{code}\n\`\`\``
**Harmful content (EN):** `❌ I can't help with that request.`
**Harmful content (中文):** `❌ 该请求超出我的能力范围，无法提供帮助。`
**Timeout (EN):** `⏱️ AI service timed out (30s). Retrying...`
**Timeout (中文):** `⏱️ AI 服务响应超时（30s）。正在重试...`
**Error (EN):** `❌ AI service unavailable. Please try again in a moment.`
**Error (中文):** `❌ AI 服务暂时不可用，请稍后再试。`

## Error Handling

| Error | Cause |
|-------|-------|
| Message is required | Empty message |
| AI service unavailable | LLM service down |
| Model not found | Invalid model name |
| Response timeout | Takes too long |

See [references/](references/) for related skills and API docs.
