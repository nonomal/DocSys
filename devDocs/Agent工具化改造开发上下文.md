# Agent 工具化改造 —— 开发上下文（Session 交接/续接文档）

> **用途**：本文件是"续接锚点"。新 session 或 compact 后，**先读这一份**即可恢复完整开发上下文，然后按需展开细节文档。
>
> **本任务把 DocSys Agent 从"单轮意图识别 + 规则路由 + chat 兜底"，升级为像 Claude Code / Copilot 一样的"工具推理 Agent"**：LLM 自主决定调用哪个工具、可连续多轮调用、失败可重试/换策略，最终能处理文档系统域内几乎所有的用户请求。

---

## 0. 续接时先读这三份（顺序）

| # | 文档 | 内容 |
|---|------|------|
| 1 | 本文件 `devDocs/Agent工具化改造开发上下文.md` | 全局状态、决策、代码地图、命令、下一步（**先读**） |
| 2 | `devDocs/Agent工具化改造开发计划.md` | 任务树（T0→T6）+ 每个叶子的详细完成状态/护栏证据（**权威进度**） |
| 3 | `.claude/agent-context.md` | Agent 模块的代码坐标、待办、部署/编译环境（**项目级上下文**） |

还有 `docs/agent/*`（agent 模块产品文档）与 `src/com/DocSystem/agent/README.md`（模块正文）。

---

## 1. 目标与边界（已定，勿重议）

- **目标**：新增 `ToolUseLoop`（REPL 循环：LLM 思考 → 决定调工具 → 执行 → 观察结果 → 再思考 → …直到给出最终回答），与旧路径并存 + 灰度切换，最终默认开启并替代单轮规则路由。
- **边界**：限定 DocSys 文档管理域（不做通用编程 Agent）；不改 `DocSysClient` 底层；不重写 `LLMService`；不改旧 `decomposeTask` 路由代码（翻正前旧路径保持原样）。
- **新旧并存**：新 `ToolUseLoop` 与旧 `decomposeTask` 路由**并存 + 灰度开关**（`agent.tool-loop.enabled` 默认 false），任何阶段可一键回退。

### ★★★ 关键架构决策（用户 2026-07-31 明确，勿违背）

**当前 Agent 的本质缺陷（用户确认）**：LLM 只当"分类器"用（`decomposeTask` 单次分类），从不当"推理引擎"用。升级方向 = 让 LLM 拥有**工具选择权 + 多步推理 + 失败重试**，工作方式对齐 Claude Code / Copilot。

**升级三要素**（对应计划 T1-T3）：
1. **Tool Schema**：把 `DocSysClient` 的 40+ API 定义为 LLM 可见的工具（name/description/parameters JSON Schema）。
2. **系统提示词**：从"意图分类提示词"改为"工具使用提示词"（描述工具 + 使用规则 + `<tool_call>` 输出格式）。
3. **ToolUseLoop**：REPL 循环替代单次分类（LLM 决策→执行→观察→再决策→最终回答）。

### ★★★ 并发安全约束（参考 LLMService 既有教训，勿违背）

`LLMService` 是单例，持有可变字段 `endpoint / defaultModel / apiKey / openAiCompatible`，`refreshFromSystemConfig` / `applyConfig` 会写这些共享字段——**存在并发竞态**。ToolUseLoop 及一切请求级配置（含工具集、模型选择）必须以**局部变量/方法参数**贯穿，不得在请求路径上写共享字段。已封装的 `chat(msg, sid, ResolvedLlmConfig)` 无状态重载就是正确范式。

### ★★★ 模型身份问题（2026-07-31 已修复，勿回退）

系统提示词曾强设 `"You are DocSys AI Assistant..."`，模型无法如实回答自身身份（会编造"基于 GPT-4 架构"）。已改为**纯场景描述 + 显式禁止编造架构信息**：

```
DocSys document management context: help users with documents, repositories, and related tasks.
Be concise and helpful. You do not know what model or architecture you run on — if asked,
say you are an assistant in DocSys and focus on the user's needs.
```

三处提示词（`chat(msg,sid,model)`、`chat(msg,sid,resolved)`、`streamChat(msg,sid,resolved)`）均已统一。**不得再引入 "You are" 身份设定**。

---

## 2. 关键决策（已定，勿重议）

1. **工具调用输出格式**：用 `<tool_call>{"name":"...","arguments":{...}}</tool_call>` XML 标签包裹（兼容性最好，不依赖特定模型原生 function calling；原生支持的模型后续可增强为 OpenAI tool_calls 格式）。
2. **工具执行引擎**：新建 `agent/tool/` 包（`ToolDefinition` / `ToolRegistry` / `ToolCallParser` / `ToolPromptBuilder`），在 `DocSysClient` 之上做封装，不动其底层。
3. **写操作安全**：写工具一律 `isWrite=true` + `needsConfirm=true`，执行前接入现有 SSE confirm 审批（复用 `AuditLogService`）。
4. **灰度策略**：`agent.tool-loop.enabled` 默认 false；翻正（T6.3）前旧 `decomposeTask` 路径一行不动，保证零回归。
5. **轮数上限**：`MAX_TURNS=10` 硬上限；相同工具重复调用防死循环；异常工具结果注入"重试/换工具"提示。
6. **护栏优先**：每个工具/组件先建独立单测（`TestToolRegistry` / `TestToolCallParser` / `TestReadTools` / `TestWriteTools`），再进端到端。

---

## 3. 改造方法论（每一步都照做）

**盘点 → 定义 → 封装 → 护栏 → 灰度接入 → 端到端验证。**

- 每接一组工具：先写 ToolDefinition → 注册进 ToolRegistry → 用真实 `DocSysClient` 跑 execute 验证 → 标记 `[UNVALIDATED]` 直到端到端通过。
- 每改一处 `LLMService`：只加方法/改提示词，不动共享字段写路径；编译用 JDK 1.8（`C:\Program Files\Java\jdk1.8.0_162\bin\javac`）。
- 每完成一个里程碑：在计划文档标 `[x]` + 更新本文件 §5/§6。

---

## 4. 代码地图（`src/com/DocSystem/agent/`）

### 4.1 现有关键类（改造基础）
| 类 | 职责 | 改造中的角色 |
|---|---|---|
| `controller/AgentController.java` | REST/SSE 入口（`/agent`）；`runCommand` / `stream` / models CRUD | 入口不变；`runCommand` 已贯通 `ResolvedLlmConfig`（2026-07-31 完成） |
| `orchestrator/MainAgent.java` | 编排：`process` → `decomposeTask` → `executeSubTasks` → 聚合 | **改造核心**：新增 ToolUseLoop 分流 |
| `orchestrator/SubAgent.java` | 按任务类型池化的执行单元（switch → handleXxx） | 工具执行的候选实现来源（可复用 handleXxx 逻辑） |
| `orchestrator/reflection/` | `ReflectionEngine` + `Replanner` + `OrchestrationLoop`（默认关） | T5.1 失败重试复用 |
| `llm/LLMService.java` | chat/stream 多模型；**三处系统提示词已软化** | ToolUseLoop 的 LLM 调用方 |
| `llm/ResolvedLlmConfig.java` | 不可变请求级模型配置 | 贯穿请求参数 |
| `llm/UserLlmModelService.java` | 用户自定义模型 CRUD + `resolve(modelId,userId)` | 前端已可选模型 |
| `client/DocSysClient.java` | **40+ `.do` API 封装**（用户/仓库/文档/搜索/RAG/管理） | **工具集的底层来源** |
| `skill/` + `skill/executor/` | `SkillExecutorRegistry`（可插拔）、`EnhancedSkillManager` | T4.4 Skill 作 Tool 暴露 |
| `core/AgentContext.java` | 会话/用户/权限上下文 | 工具执行上下文 |
| `monitoring/RateLimitService.java` + `controller/AuditLogService.java` | 限流 + 写审计 | T4 安全融合 |

### 4.2 计划新增类（`agent/tool/` 包）
| 类 | 职责 | 计划节点 |
|---|---|---|
| `tool/ToolDefinition.java` | name/description/parameters(JSON Schema)/executor/isWrite/needsConfirm | T1.2 |
| `tool/ToolRegistry.java` | register/list/find/execute；角色过滤 | T1.3 + T3.5 |
| `tool/ToolCallParser.java` | 解析 `<tool_call>...` 输出，容错 | T1.5 |
| `tool/ToolPromptBuilder.java` | 渲染工具系统提示词 | T1.4 |
| `orchestrator/ToolUseLoop.java` | REPL 循环主引擎 | T2.1 |
| `tool/` 下各工具组定义 | 只读/写/RAG/用户 | T3.x |

---

## 5. 已完成进度（截至 2026-07-31）

> **当前状态**：T0 未开始。以下为"已具备"的改造基础（历史会话完成，勿重做）。

### 5.1 模型选择贯通自然语言链路（2026-07-31 完成）
- `LLMService.chat(msg, sid, ResolvedLlmConfig)` 无状态重载已新增（只用局部变量，不写共享字段）。
- 调用链贯通：`AgentController.execute(modelId) → runCommand(..., resolvedLlm) → MainAgent.process(..., resolvedLlm) → executeSubTasks(..., resolvedLlm) → SubAgent.execute(..., resolvedLlm) → handleChat/GenerateSummary/SearchAndAnswer → chat(msg, sid, resolved)`。
- `ExecuteRequest` 新增 `modelId` 字段；`/executeSmart` 不传时 `resolvedLlm=null` 回退系统默认（等价现状）。
- 编译通过（JDK 1.8，5 个 `.class` 更新）；`UserCustomLlmModelRepositoryMapper.xml` 已同步到 `WebRoot/WEB-INF/classes/mapper/`。

### 5.2 系统提示词软化（2026-07-31 完成）
- 三处提示词统一改为纯场景描述 + 禁止编造架构信息（见 §1 ★★★）。
- 编译通过 + Tomcat work 缓存已清（`C:/docsysRel/docsys-WDK/docsys/tomcat/work/Catalina/localhost/`）。

### 5.3 已验证的事实（改造依据）
- 意图识别**不只支持 chat**：多层管道（复合命令 → LLM NLU → Skill trigger → regex → chat 兜底），支持 list_repos/list_docs/search/generate_summary/search_and_answer/whoami/help/web_search 等 10+ 类。
- LLM 当前只当分类器 + 兜底对话用，**无工具选择权、无多步推理、无失败重试**（用户确认这是要升级的缺陷）。
- `DocSysClient` 40+ API 全部可用；`LLMService` 多模型可用；`ReflectionEngine` 实装完整但默认关。

---

## 6. 下一步 + 剩余大块

- **当前里程碑**：T1 骨架与护栏（未开始）。
- **下一步（T1.1）**：盘点 `DocSysClient` 全部 API → 产出工具清单 `devDocs/tool-inventory.md`。
- **再下一步（T1.2-T1.5）**：`ToolDefinition` → `ToolRegistry` → `ToolPromptBuilder` → `ToolCallParser` → 护栏单测。
- **T2（核心）**：`ToolUseLoop` 最小闭环（单轮循环 + 历史管理 + 轮数控制 + 灰度开关接入 `MainAgent.process`）。
- **T3-T6**：工具集全量接入 → 安全融合 → 高级能力 → 验收翻正。

### 关键环境/命令（勿另搞一套）
- 编译器：`"C:\Program Files\Java\jdk1.8.0_162\bin\javac" -encoding UTF-8 -cp "WebRoot/WEB-INF/classes;WebRoot/WEB-INF/lib/*" -d WebRoot/WEB-INF/classes <改动的.java>`
- 编译输出：`WebRoot/WEB-INF/classes/`（Eclipse/Tomcat 部署目录）
- 部署：`docsys_start.bat`（勿用 docsys_restart.bat）；编译后若启动报类加载错，清 `C:/docsysRel/docsys-WDK/docsys/tomcat/work/Catalina/localhost/` 再启
- 前端：只动 `WebRoot/web/agent/index.html`
- Mapper XML 改动后同步到 `WebRoot/WEB-INF/classes/mapper/`

---

## 7. 参考（勿重读大文件，定点查）

- `.claude/agent-context.md`：Agent 模块代码坐标、待办、部署顺序。
- `src/com/DocSystem/agent/README.md`：模块正文（架构/技能/可观测/安全审计）。
- `Docs/agent/architecture.md`：调用链速查。
- 历史教训：不要反复重读已确认的大文件（如 SubAgent.java 1500+ 行）导致上下文膨胀；需要签名时定点读对应方法段即可。
