---
name: list_models
description: List all available AI models for chat and RAG
category: ai
version: 1.1.0
author: DocSys Team
permissions:
  - read
tags: [ai, models, list, available-models]
---

# List AI Models

Display all available AI models that can be used for chat and RAG.

## Triggers

AI models, 模型列表, 可用模型, models, list models, 查看AI模型, 支持哪些模型, available models, what models, list AI models, 查看可用模型, AI模型有哪些, 支持哪些LLM, list all AI models, what LLM models, 可用的大模型, 大模型列表, 模型能力, 模型对比

## CLI Command

```bash
docsys models
docsys models --verbose
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| --verbose | flag | no | Show detailed model capabilities |

## Examples

### Example 1 (基础列表)
User: "what AI models are available"
Skill triggers → calls: `docsys models`
Output: gpt-4o (OpenAI, default), gpt-3.5-turbo (OpenAI), claude-3 (Anthropic)

### Example 2 (详细模式)
User: "show me all available models in detail"
Skill triggers → calls: `docsys models --verbose`
Output: Name, Provider, Context Window, Supports Vision, Supports Function Calling, Default

### Example 3 (中文场景)
User: "支持哪些大模型"
Skill triggers → calls: `docsys models`
Output: 可用模型：gpt-4o (OpenAI)、文心一言 (Baidu)、通义千问 (Alibaba)

### Example 4 (错误场景)
User: "list available AI models"
Skill triggers → calls: `docsys models`
Output: 错误：No AI models configured（未配置模型时返回此错误）

### Example 5 (内置推荐规则)
User: "哪个模型最适合写代码"
Skill triggers → calls: `docsys models --verbose` → then apply recommendation rules:
| Use Case | Recommended | Why |
|----------|-------------|-----|
| Code generation | gpt-4o / claude-3 | High context_window + strong reasoning |
| Long document summarization | gpt-4o | Largest context_window |
| Fast simple Q&A | gpt-3.5-turbo | Lowest latency |
| Chinese content | 文心一言 / 通义千问 | Native Chinese training |
| Function calling | claude-3 / gpt-4o | Native function_calling support |
Output: Recommended model + capability fields

## Output Format

Success: Model list with name, provider, is_default. With --verbose: + context_window (tokens), capabilities (vision, function_calling).
Error: "No AI models configured", "Connection failed", "API timeout"

## Error Handling

| Error | Cause |
|-------|-------|
| No AI models configured | No AI models available in config |
| Connection failed | DocSystem unreachable |
| API timeout | AI service timeout |

See [references/](references/) for related skills and API docs.
