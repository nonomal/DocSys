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

> **当前状态**：T1-T4 全部闭环 + T5.1/T5.2 完成 + **T7 流式体验升级全部完成（2026-07-31 部署验收通过）**（灰度开关已翻正）。护栏 168 断言全绿。下一步 = T5.3/T5.4 增强 + 后续候选。

### 5.1 模型选择贯通自然语言链路（2026-07-31 完成）
- `LLMService.chat(msg, sid, ResolvedLlmConfig)` 无状态重载已新增（只用局部变量，不写共享字段）。
- 调用链贯通：`AgentController.execute(modelId) → runCommand(..., resolvedLlm) → MainAgent.process(..., resolvedLlm) → executeSubTasks(..., resolvedLlm) → SubAgent.execute(..., resolvedLlm) → handleChat/GenerateSummary/SearchAndAnswer → chat(msg, sid, resolved)`。
- `ExecuteRequest` 新增 `modelId` 字段；`/executeSmart` 不传时 `resolvedLlm=null` 回退系统默认（等价现状）。
- 编译通过（JDK 1.8，5 个 `.class` 更新）；`UserCustomLlmModelRepositoryMapper.xml` 已同步到 `WebRoot/WEB-INF/classes/mapper/`。

### 5.2 系统提示词软化（2026-07-31 完成）
- 三处提示词统一改为纯场景描述 + 禁止编造架构信息（见 §1 ★★★）。
- 编译通过 + Tomcat work 缓存已清（`C:/docsysRel/docsys-WDK/docsys/tomcat/work/Catalina/localhost/`）。

### 5.3 T1 Tool 基础设施（2026-07-31 完成，护栏全绿）
- **T1.1 盘点**：`DocSysClient` 35 个业务 API（17 只读 + 14 写 + 3 会话 + 1 危险）。清单 `devDocs/tool-inventory.md`（含 JSON Schema + 注册决策）。
- **T1.2 `tool/ToolDefinition.java`**：Builder 模式 + 校验（空 name/null executor/needsConfirm 需 isWrite）。配套 `ToolExecutor`（函数式）+ `ToolResult`（成功/失败 + 文本摘要 + 数据）。
- **T1.3 `tool/ToolRegistry.java`**：线程安全注册表 + required 校验 + adminOnly 过滤 + 异常包装。单例 `getInstance()`。
- **T1.4 `tool/ToolPromptBuilder.java`**：工具使用提示词渲染（场景上下文 + 工具列表 + 6 条使用规则 + 截断保护）。
- **T1.5 `tool/ToolCallParser.java`** + `ToolCall`：解析 `<tool_call>{"name":"...","arguments":{...}}</tool_call>`；多调用/畸形→null 重试/arguments 字符串容错。
- **T1.6 护栏**：`TestToolRegistry` **20/20**、`TestToolCallParser` **16/16**（手写 main，无 Spring 依赖，编译通过）。
- ⚠️ 注：测试运行时 log4j 报 "Unsupported encoding" 警告（编码配置问题，不影响断言结果）。

### 5.4 T2 ToolUseLoop 最小闭环（2026-07-31 完成，护栏全绿）
- **T2.1 `orchestrator/ToolUseLoop.java`**：REPL 循环引擎（MAX_TURNS=10）。`LlmCaller` 函数式接口抽象 LLM 调用（默认包装 `LLMService.chat(List, resolved)`，测试注入假实现）。配套 `ToolUseResult`（success/message/transcript/turns/toolCalls/maxTurnsExceeded）。
- **T2.2 消息管理**：ToolUseLoop 内部独立管理消息列表（不用 LLMService.conversationHistory）；工具结果以 **role=user + `[TOOL_RESULT tool=name]` 标记**回灌（兼容不支持 tool role 的模型）；`trimTranscript` 超 30 条裁剪最早工具结果。
- **T2.3 防死循环**：MAX_TURNS=10 硬上限 + 连续相同工具+参数 ≥3 次注入"换思路"提示（`MAX_IDENTICAL_CALLS=3`）+ 连续畸形 ≥3 次终止（`MAX_MALFORMED=3`）。
- **T2.4 灰度接入**：`MainAgent` 新增 `@Value("${agent.tool-loop.enabled:false}")`；process() 在 decomposeTask 前分流：ON → `ToolUseLoop.forLlmService(llmService, registry, resolvedLlm, isAdmin)`，**成功返回、失败回退旧路径**。`extractIsAdmin`（Admin 用户名即管理员）。开关 OFF 时旧路径零改动。
- **T3.1 只读工具 `tool/DocSysToolFactory.java`**：12 个只读工具（get_login_user/list_repos/get_repos/list_docs/get_doc/get_doc_history/search_docs/list_ai_models/get_sys_config/get_banner_config/get_doc_share_list/query_backup_status），闭包绑定 per-request DocSysClient + JSON Schema + 结果截断 4000 字符。
- **护栏**：`TestToolUseLoop` **32/32**（多轮链/单轮/未知工具恢复/畸形重试/连续畸形终止/超轮数/重复调用检测/上下文裁剪/adminOnly 过滤）。
- ⚠️ 待做：`TestReadTools` 护栏（需真实 DocSysClient 会话）；真实 LLM 端到端验证（开灰度开关后人工跑通）。

### 5.5 T3.2 写操作工具 + T4.1 确认机制（2026-07-31 完成，护栏全绿）
- **`DocSysToolFactory.createFullRegistry`**：24 个工具（12 只读 + 12 写：create_repos/delete_repos/update_repos/create_doc/delete_doc/rename_doc/move_doc/copy_doc/lock_doc/unlock_doc/create_doc_share/backup_repos），写工具全部 `isWrite+needsConfirm`。
- **`tool/WriteConfirmGate.java`**：写操作确认门抽象（函数式，NOOP 放行）。
- **`tool/AuditWriteConfirmGate.java`**：对接 AuditLogService——`createPendingEntry` 生成 confirmToken → 日志打印 → 轮询等 /confirm → 用新增 `AuditLogService.getStatusByConfirmToken()` **正确区分 approve/reject**（⚠️ 现有 `AgentController.waitForConfirmation` 存在把 approve 当拒绝的隐患，未在本次范围内修复，可留意）。
- **`ToolRegistry.setConfirmGate()`**：needsConfirm 工具执行前拦截。
- **`MainAgent`**：注入 AuditLogService；ToolUseLoop 路径改 `createFullRegistry` + `AuditWriteConfirmGate`（`agent.tool-loop.confirm-timeout` 默认 120s）。
- **护栏 `TestWriteTools`** **54/54**：写工具 flags / gate 触发/拒绝/批准/NOOP / 只读不触发 / rag_chat+ai_chat 注册检查。
- ⚠️ **已知潜伏 bug 已修复**：`DocSysToolFactory.props()` 曾缺 key（运行时 ClassCastException），改为属性构建器自带 key。
- ⚠️ 待做：confirmToken 的**前端 SSE 弹窗推送**（当前仅日志/审计可见，ToolUseLoop 在 /execute 非 SSE 路径）。

### 5.6 T3.3-T3.5 工具集收尾（2026-07-31 完成）
- **T3.3 RAG/搜索**：`rag_chat`（/Query/addDocSysRagMessage）+ `ai_chat`（/Repos/AIChat），只读不触发确认门，String 返回经 `fmtString` 截断。
- **T3.4 会话工具**：决策落地——仅注册只读 `get_login_user`；**login/logout/register 默认不注册**（Agent 复用共享会话，LLM 不应有登录/登出能力）。
- **T3.5 可见性白名单**：`ToolRegistry.listForUser(isAdmin, readOnly)` 两级过滤（adminOnly + 写工具）+ `execute()` 校验 adminOnly。⚠️ readOnly 角色判定未接真实角色服务（MainAgent 默认 false）。
- 护栏：`TestToolRegistry` **24/24**、`TestWriteTools` **54/54**（含新用例）。
- **工具全景**：26 个 = 14 只读（含 search_docs/rag_chat/ai_chat）+ 12 写（全部 needsConfirm）。

### 5.7 T4.2-T4.4 安全融合（2026-07-31 完成）
- **T4.2 审计覆盖**：`AuditLogService.record()`（终端 COMPLETED/FAILED 条目）+ `ToolRegistry.ToolExecutionListener`（每次 execute 后回调）+ MainAgent 接线（写工具 → audit 落库，参数经 sanitizeParams 脱敏）。护栏 `TestToolRegistry.testExecutionListener`。
- **T4.3 监控**：MainAgent 注入 AgentMetrics——`agent.tool.call`（tool/write/success）+ `agent.tool.loop`（success/turns）+ `agent.execution`(tool_loop)。⚠️ RateLimitService 限额需 client IP（AgentController 层有），待 SSE 路径接入后接线。
- **T4.4 Skill 协同**：`DocSysToolFactory.runSkillTool()` 通用技能工具（skillId+params → SkillExecutorRegistry.execute，isWrite+needsConfirm）；MainAgent 有 SkillExecutorRegistry 时自动注册 `run_skill`。⚠️ SkillIntentRegistry 是 stub（无外部技能），内置技能与直接工具重叠。
- **护栏全景**：`TestToolRegistry` **29/29**、`TestToolCallParser` **16/16**、`TestToolUseLoop` **32/32**、`TestWriteTools` **54/54** = **131 断言全绿**。

### 5.8 T4.1 收尾 + T5.1 + 开关翻正（2026-07-31 完成）
- **T4.1 SSE 确认推送**：`ConfirmEventSink` 接口 + `AuditWriteConfirmGate.setConfirmEventSink()`；`AgentController.stream()` 在 tool-loop 开启时调 `mainAgent.runToolUseLoop(command, ..., sseSink)`（SSE 路径推送 `{"type":"confirm",...}`）。`MainAgent.runToolUseLoop(...)` 抽取为**公共可复用方法**（process / AgentController SSE 共用），含 confirmSink 参数。
- **T5.1 失败重试**：ToolUseLoop 失败（超轮数/畸形终止）时注入"直接回答"提示重试一次（有界重试；完整 ReflectionEngine 重规划留作增强）。
- **灰度开关翻正**：`MainAgent` + `AgentController` 的 `agent.tool-loop.enabled` 默认值改为 **true**（测试阶段，可用 `-D` 回退）。
- ⚠️ 已知隐患：旧路径 `AgentController.waitForConfirmation` 把 approve 当拒绝（新 ToolUseLoop 路径已规避，旧路径待修）。

### 5.9 T5.2 会话记忆（2026-07-31 完成）
- **T5.2a 会话历史持久化 + 续接入口**：
  - 新表 `agent_session_messages`（MySQL+SQLite 双方言 DDL 已加入 DatabaseInitializer）
  - `session/SessionMessageEntity` + `SessionMessageRepository` + `src/mapper/SessionMessageRepositoryMapper.xml`（已部署）+ `session/ConversationHistoryService`（appendMessage/saveExchange/getHistory/countBySessionId/clear/deleteSession）
  - `SessionService` 扩展：`listByUsername`/`createSession`/`updateTitleIfEmpty`（标题=首条消息截断存 metadata JSON）+ `titleFromMetadata`/`truncateTitle` 静态工具
  - AgentController 新端点：`GET /sessions`、`POST /sessions`、`GET /sessions/{id}/messages`、`DELETE /sessions/{id}`
  - 保存钩子：stream() isAiChat/toolLoop/legacy 三路径 + /execute 路径
  - 前端 index.html：会话 API + `loadSessionsFromServer()` + 新会话走服务端 + 点击加载历史 + 无会话自动创建 + 完成后刷新 + 删除按钮/标题 CSS
- **T5.2b 跨请求上下文延续**：`ToolUseLoop.run(userQuery, priorHistory)` + `MainAgent.runToolUseLoop(..., historySessionId)` 加载最近 20 条 user/assistant 作为 LLM 上下文（`loadSessionHistory`）。
- 护栏：`TestToolUseLoop` 新增 `testHistoryInjection` 4 项 → **36/36**。
- **护栏全景**：`TestToolRegistry` **29/29**、`TestToolCallParser` **16/16**、`TestToolUseLoop` **36/36**、`TestWriteTools` **54/54** = **135 断言全绿**。

### 5.10 T7 流式体验升级（2026-07-31 完成 T7.1-T7.3）
- **T7.1 后端流式化**：
  - `llm/StreamChunk.java`（新）：text/reasoning/done 三分片类型。
  - `LLMService.streamChatChunks(List, resolved)`：多消息流式重载（对偶 chat(List)），OpenAI 兼容流 delta 提取 `reasoning_content`/`reasoning` 与 `content` 分离；Ollama 流 message 同名字段；末尾必有 done 标记。
  - `ToolUseLoop`：`StreamingLlmCaller` + `StreamSink`（onReasoning/onText/onToolCall/onToolResult/onRetry，默认空实现）；`runStreaming(query, history, sink)`；run/runStreaming 共用 `runInternal`（TurnRunner 抽象）；`forLlmServiceStreaming` 双通道工厂；无流式通道自动回退非流式。
  - `MainAgent`：`runToolUseLoopStreaming(...)`（流式运行 + onRetry 通知 + 失败重试保持）；`buildToolLoop(...)` 抽取流式/非流式共用。
  - `AgentController`：`runToolLoopStreamingWithSse`（StreamSink→SSE 事件：reasoning/text/tool_call/tool_result/retry；全程累积 reasoning 供持久化）；`stream()` ToolUseLoop 路径真流式 + done 携带 meta；**isAiChat 路径也升级**为 streamChatChunks（DB 历史构建消息列表，reasoning/text 事件）；确认事件 confirm 保持。
- **T7.2 前端**（只动 `WebRoot/web/agent/index.html`）：
  - 真流式渲染（text/chunk 事件 → liveText 实时追加；done 落 content）；reasoning 独立灰色累积。
  - 工具进度卡片 `renderToolCard`（running 转圈/成功/失败三态 + 参数 + 结果摘要/红字）；tool_call 前文本剥离 `<tool_call>` XML 收进处理容器。
  - 可折叠处理过程容器 `<details class=process-container>`（reasoning 灰色小字 + 中间文本 + 工具卡片，生成中自动展开）；深色主题适配。
  - **补写操作确认弹窗**（`{type:confirm}` → 批准/拒绝 POST /confirm，此前前端一直未接）。
- **T7.3 reasoning 持久化**：`ConversationHistoryService.saveExchange(sid, user, assistant, reasoning)` 重载——reasoning 以 role=reasoning 消息落库（表 role VARCHAR(16) 无约束，无需 DDL）；三处保存钩子均传 reasoning；前端 loadSessionMessages 合并到其后 assistant 灰色展示；`MainAgent.loadSessionHistory`/isAiChat 消息构建只取 user/assistant（回放剥离不回灌模型）。
- **护栏**：新增 `TestToolUseLoopStreaming` **26/26**（流式单轮/多轮工具链/reasoning 分离/工具失败/回退/重复调用提示）。**护栏全景 = 29+16+36+26+52 = 159 断言全绿**。
- ⚠️ 编译通过（JDK 1.8，6 个 .java 更新 + StreamChunk 新增）；`runToolLoopWithSse`（旧非流式）已成死代码保留（无引用）；无新 Mapper/DDL。
- ⚠️ 待做：T7.4 部署验收（docsys_start.bat 重启 + 真实 LLM 三类场景 + 首 token 延迟对照）。

### 5.11 T5.3 复杂任务多步编排验证（2026-07-31 完成）
- 真实 LLM 验证通过：流式 `list_repos→search_docs→list_docs` 自主串联 + 非流式 6 工具/9 轮，产物正确。
- **工具 bug 修复**（DocSysClient）：`getDocList` 端点改 `/Repos/getSubDocList.do`（子文件夹必须传 path/docId，pid 被忽略）；`searchDocs` 参数 `vid`→`reposId`（原全局搜索超时）；`getDoc`/`getDocHistory` 参数 `vid`→`reposId`；`getDocShareList` 无参数。规律：**Repos 端点用 vid，Doc 端点用 reposId**。
- **模型输出容错**（ToolCallParser/ToolPromptBuilder）：支持 Anthropic XML 格式回退；含 `<tool_call` 标记但无法解析→按畸形重试；提示词禁止 `<invoke>/<parameter>`。
- ⚠️ **DocSystem 核心 bug**（非 Agent）：`getDoc.do` 全 500 NPE（docSysGetDoc 查库空）；`createDocShare.do` 端点不存在。已记录待修。
- 护栏：TestToolCallParser 16→23（+7）；**护栏全景 = 29+23+36+26+52+9 = 175 全绿**。

### 5.12 T8.1 修复 getDoc.do NPE（2026-07-31 完成）
- **DocController.getDoc.do NPE 两处修复**：4301 行 `FileUtil.getFileSuffix(name)` 空指针 → `(name != null)` 守卫；4301/4324 行 `docType == 1/2/3` 拆箱 NPE → `Integer.valueOf(n).equals(docType)` 安全比较。
- **DocSysClient.getDoc 加 `docType=1`**：让服务端返回 docText（实测 buildDocSystem.bat docText=4226 字符；记录1.docx docText=123 字符）。`get_doc` 工具 schema 改 required=[vid,path,name] + 描述引导"必须同时传 path 和 name（来自 list_docs 结果）"。
- **端到端多步链验收通过**：真实 LLM `list_repos → list_docs（vid=8, path=DocSys）→ get_doc（记录1.docx）→ 总结` 完整跑通，SSE turns=4/toolCalls=3，正确总结 DTU 唤醒配置要点。
- **⚠️ 编译部署教训**：Spring Controller 编译必须加 `-parameters -g`（否则 `IllegalArgumentException: Name for argument type not available`）；Agent 包内文件不需要。
- **⚠️ 部署/热重载教训（重要）**：`wtpwebapps` 类文件时间戳/哈希与工作区一致，但 Tomcat JVM 仍可能运行旧类（热重载未生效）。**可靠验证手段**：在循环里加文件转储日志（本次用 `D:/tmp_loop_debug.log`）确认运行时真实行为；强制触发重载 = 重新复制 .class（更新时间戳）。
- **解析器加固（验收中发现）**：模型漂移输出**复数 `<tool_calls>`**（Anthropic 风格）→ 旧正则只匹配单数被当最终回答。修复：`TOOL_CALL_PATTERN` 改 `<(tool_call|tool_calls)>(.*?)</(tool_call|tool_calls)>`；`looksLikeMalformedToolCall` 增补 `<tool_calls`/`</tool_calls>`。TestToolCallParser 23→**32 全绿**。

### 5.5 已验证的事实（改造依据）
- 意图识别**不只支持 chat**：多层管道（复合命令 → LLM NLU → Skill trigger → regex → chat 兜底），支持 list_repos/list_docs/search/generate_summary/search_and_answer/whoami/help/web_search 等 10+ 类。
- LLM 当前只当分类器 + 兜底对话用，**无工具选择权、无多步推理、无失败重试**（用户确认这是要升级的缺陷）。
- `DocSysClient` 40+ API 全部可用；`LLMService` 多模型可用；`ReflectionEngine` 实装完整但默认关。

---

## 6. 下一步 + 剩余大块

- **当前里程碑**：T7 流式 + T5.3 多步编排 + **T8.1 getDoc.do NPE 修复全部完成（2026-07-31）**。
- **决策（2026-07-31，用户明确）**：旧路径（decomposeTask/SubAgent/LLMIntentParser 等）经评估**仍有使用，保留不动**（不删除）；**取消 T6.1/T6.2 新旧对比测试**；T6.4 长尾回归降级为可选（旧路径保留为兜底，无能力损失）。
- **下一步 = T8 后续增强（已排期按序执行）**：~~T8.1 修 getDoc.do NPE~~（✅ 完成）→ **T8.2 修 create_doc_share 端点** → T8.3 Memory 工具 → T8.4 Web Search → T8.5 Step 审计 → T8.6 Admin 配置提示词。
- **⚠️ 回退开关**：`-Dagent.tool-loop.enabled=false` 或 Spring 属性可一键回退旧路径（旧路径保留即此开关仍有效）。

### 关键环境/命令（勿另搞一套）
- 编译器：`"C:\Program Files\Java\jdk1.8.0_162\bin\javac" -encoding UTF-8 -cp "WebRoot/WEB-INF/classes;WebRoot/WEB-INF/lib/*" -d WebRoot/WEB-INF/classes <改动的.java>`
- 运行测试：`"C:\Program Files\Java\jdk1.8.0_162\bin\java" -cp "WebRoot/WEB-INF/classes;WebRoot/WEB-INF/lib/*" com.DocSystem.agent.tool.TestToolRegistry`（log4j 编码警告可忽略）
- 编译输出：`WebRoot/WEB-INF/classes/`（Eclipse/Tomcat 部署目录）
- 部署：`docsys_start.bat`（勿用 docsys_restart.bat）；编译后若启动报类加载错，清 `C:/docsysRel/docsys-WDK/docsys/tomcat/work/Catalina/localhost/` 再启
- 前端：只动 `WebRoot/web/agent/index.html`
- Mapper XML 改动后同步到 `WebRoot/WEB-INF/classes/mapper/`

### ⚠️ 部署约束（2026-07-31 用户明确）
- **不要直接进行 Tomcat 部署/复制 .class 到 wtpwebapps**（直接部署会导致应用出错）。
- 只保证**编译通过**（javac 到 `WebRoot/WEB-INF/classes`）；需要部署时**通知用户手动操作**。
- 测试类可本地 `java` 运行（不涉及部署）。

---

## 7. 参考（勿重读大文件，定点查）

- `.claude/agent-context.md`：Agent 模块代码坐标、待办、部署顺序。
- `src/com/DocSystem/agent/README.md`：模块正文（架构/技能/可观测/安全审计）。
- `Docs/agent/architecture.md`：调用链速查。
- 历史教训：不要反复重读已确认的大文件（如 SubAgent.java 1500+ 行）导致上下文膨胀；需要签名时定点读对应方法段即可。
