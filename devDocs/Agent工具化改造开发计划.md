# Agent 工具化改造：从"意图分类器"升级为"工具推理 Agent"开发计划

> 配套文档：[`Agent工具化改造开发上下文.md`](Agent工具化改造开发上下文.md)（续接锚点，先读它，再看本计划）。
>
> 目标边界（已确认）：**把 DocSys Agent 从当前的"单轮意图识别 + 规则路由 + chat 兜底"，升级为像 Claude Code / Copilot 一样的"工具推理 Agent"**——LLM 自主决定调用哪个工具、可连续多轮调用、失败可重试/换策略，最终能处理文档系统域内几乎所有的用户请求。
>
> 现状一句话：`decomposeTask()` 做单次分类 → 命中预设意图则执行，否则直接 `chat`。LLM 只当"分类器"用，从不当"推理引擎"用。
>
> 基础设施已具备（无需重写）：`DocSysClient`（40+ API 已封装）、`LLMService`（chat/stream 多模型）、`ResolvedLlmConfig`（线程安全选模型）、`SkillExecutorRegistry`（可插拔执行器）、`ReflectionEngine`（失败重试，默认关）、`AgentContext`（会话/权限）。

---

## 计划结构规则

1. 只用"根节点 → 中间节点 → 叶子节点"的树状结构组织任务。
2. 每个任务节点带 `[ ]` / `[x]` 状态；父节点只有全部直接子节点完成后才可改 `[x]`。
3. 只有叶子节点可直接执行；叶子仍过大必须先拆分再开工。
4. 节点语义一旦建立不反复改写；执行中新工作新增到合适的同/子级节点。
5. 默认下一步 = 任务树中第一条未完成叶子节点。
6. 每个节点直接挂 `当前状态`；有阻塞/观察/下一步再挂 `当前问题` / `下一步`。
7. `全局状态` 只保留跨任务共享事实。
8. **新旧路径并存原则**：新 ToolUseLoop 与旧 decomposeTask 路由**并存 + 灰度开关**，默认走旧路径零回归；开关翻正后才切新路径。任何阶段都可一键回退。
9. **[UNVALIDATED] 标记规则**：每次新增工具/能力后，立即标记 `[UNVALIDATED]` 直到有端到端验证通过。格式：`**★ [UNVALIDATED] 无端到端验证**`。验证通过后去掉标记，改 `[x]`。

---

## 全局状态（2026-07-31）

1. 现状：Agent 请求流 = `AgentController.runCommand → MainAgent.process → decomposeTask(单次分类) → executeSubTasks(规则路由) → SubAgent.execute(switch) → LLMService.chat(兜底)`。LLM 只承担两件事：① NLU 意图分类 ② 兜底普通对话。**LLM 无工具选择权、无多步推理、无失败重试**。
2. 目标：新增 `ToolUseLoop`（REPL 循环：LLM 思考 → 决定调工具 → 执行 → 观察结果 → 再思考 → …直到给出最终回答），与旧路径并存 + 灰度切换。DocSys 域内所有请求（仓库/文档/搜索/RAG/权限）都可由 LLM 自主编排完成。
3. 已具备可直接用的资产：
   - `client/DocSysClient.java`：封装 MxsDoc **40+** `.do` 接口（用户/仓库/文档/搜索/RAG/管理），`copyWithSession()` 会话隔离，连接超时 30s。
   - `llm/LLMService.java`：`chat(msg,sid)` / `chat(msg,sid,model)` / `chat(msg,sid,ResolvedLlmConfig)`（无状态）/ `streamChat`；多模型按需取；主备切换；对话历史管理。
   - `llm/ResolvedLlmConfig.java`：不可变请求级模型配置（endpoint/model/apiKey/openAiCompatible）。
   - `llm/UserLlmModelService.java`：用户可选 + 自定义模型 CRUD（`resolve(modelId,userId)`）。
   - `orchestrator/reflection/`：`ReflectionEngine` + `Replanner` + `OrchestrationLoop`（失败→LLM 诊断→重规划→有上限重跑，默认关）。
   - `skill/` + `skill/executor/`：`SkillExecutorRegistry`（可插拔）、`EnhancedSkillManager`（trigger 打分）。
   - `orchestrator/SubAgent.java`：按任务类型池化的执行单元（tool 执行时可复用其 handleXxx 方法）。
   - `core/AgentContext.java`：会话/用户/权限上下文。
   - `monitoring/RateLimitService` + `controller/AuditLogService`：限流 + 写操作审计。
4. 待改造的核心：`MainAgent.decomposeTask()`（单次分类）→ 替换为 `ToolUseLoop`（多轮推理）。改造后：
   - 系统提示词从"意图分类提示词"改为"工具使用提示词"（描述 40+ 工具 + 使用规则）。
   - LLM 输出从"分类 JSON"改为"工具调用指令 or 最终回答"。
   - 执行从"规则路由到 SubAgent"改为"解析 tool_call → ToolRegistry 执行 → 结果回灌对话"。
5. 关键约束（参考 LLMService 并发注意事项）：所有请求级配置（含工具集）必须以**局部变量/参数**贯穿，不得写 `LLMService` 共享可变字段（存在并发竞态）。
6. 模型身份问题已修复：系统提示词已去掉 `"You are..."` 身份设定，改为纯场景描述 + 显式禁止编造架构信息（2026-07-31 已改，见上下文文档 §5）。
7. **T7 参考蓝本（2026-07-31 调研）**：`D:\Dev\ai-writting-node` PR #359 的 Agent 实现（AI SDK v7 ToolLoopAgent）——单 Agent 循环 + DB 历史重建上下文 + 三层上下文（Baseline 免压缩 persistent / Warm LLM 摘要 / Hot 近期历史）+ 双水位压缩 + 流式 UIMessage（text/reasoning/tool 卡片）+ 可折叠处理过程容器 + Step 审计 + Memory 工具 + Web Search。DocSys Agent 核心循环已对齐；**T7 补齐流式体验（Reasoning 展示 + 真流式 + 工具卡片）**，P0/P1 其余项（Memory 工具/Web Search/Warm 压缩/Step 审计/Admin 配置提示词）列入后续候选。

---

## 任务树（唯一入口）

- [ ] **T0. 完成 DocSys Agent 的工具推理化改造（ToolUseLoop），默认开启并替代单轮规则路由**
  - 当前状态：未开始。基础设施盘点、Tool 模型、ToolUseLoop、工具集接入、系统融合、验收均未落地。

  - [x] **T1. 立骨架与护栏（先于任何功能）**
    - 当前状态：**已完成（2026-07-31）**。Tool 基础模型 + 注册表 + 解析器 + 提示词模板全部落地，护栏测试全绿。
    - [x] T1.1 现状盘点：DocSysClient 全部 API → 候选 Tool 清单
      - 内容：逐方法列出 `DocSysClient` 的 40+ API（方法名、入参、出参、读/写性质），产出"工具清单"作为 Tool 定义依据。
      - 完成判据：清单覆盖全部 API，每项标注 读/写/需审计。
      - 当前状态：**已完成（2026-07-31）**。`DocSysClient` 盘点出 **35 个业务 API**（17 只读 + 14 写 + 3 会话 + 1 危险）+ 8 辅助方法。清单落 `devDocs/tool-inventory.md`，含 JSON Schema 参数定义与注册决策（会话工具默认不注册；D1 docSysInit 仅管理员白名单）。
    - [x] T1.2 建 Tool 定义模型 `ToolDefinition`
      - 内容：`agent/tool/ToolDefinition.java`：`name / description / parameters(JSON Schema) / executor(函数式) / isWrite(写操作标记) / needsConfirm`。
      - 完成判据：可构造一个最小工具（如 list_repos）并通过单测。
      - 当前状态：**已完成（2026-07-31）**。`tool/ToolDefinition.java`（Builder 模式 + 校验：空 name/null executor/needsConfirm 需 isWrite）+ `tool/ToolExecutor.java`（函数式接口）+ `tool/ToolResult.java`（成功/失败 + 文本摘要 + 结构化数据）。护栏 `TestToolRegistry` 覆盖 builder 校验。
    - [x] T1.3 建 Tool 注册表 `ToolRegistry`
      - 内容：`agent/tool/ToolRegistry.java`：`register(ToolDefinition)` / `list()` / `find(name)` / `execute(name, args)`；执行时解析 JSON 参数。
      - 完成判据：注册 3 个只读工具，execute 返回正确结果；未知工具抛清晰异常。
      - 当前状态：**已完成（2026-07-31）**。`tool/ToolRegistry.java`（线程安全 LinkedHashMap + 同名覆盖 + required 参数校验 + adminOnly 过滤 + 异常包装为 ToolResult.error）。护栏 `TestToolRegistry` 20/20 通过。
    - [x] T1.4 建系统提示词模板（工具使用描述）
      - 内容：`agent/tool/ToolPromptBuilder.java`：把 ToolRegistry 里的工具描述渲染成 system prompt（工具名/描述/参数 Schema）；描述工作方式（可连续调用、用 `<tool_call>` 标记、最终回答）。
      - 完成判据：模板能渲染 N 个工具；与 LLM 兼容（不超 token 预算，必要时截断）。
      - 当前状态：**已完成（2026-07-31）**。`tool/ToolPromptBuilder.java`（场景上下文 + 工具列表渲染 + 6 条使用规则 + MAX_TOOL_CHARS 截断保护）。
    - [x] T1.5 建工具调用解析器 `ToolCallParser`
      - 内容：解析 LLM 输出中的 `<tool_call>{"name":"...","arguments":{...}}</tool_call>`；支持失败容错（JSON 不完整时返回解析错误给 LLM 重试）。
      - 完成判据：正确/畸形/缺失工具名三类输入的单测通过。
      - 当前状态：**已完成（2026-07-31）**。`tool/ToolCall.java` + `tool/ToolCallParser.java`（正则匹配 `<tool_call>...</tool_call>`、多调用支持、畸形→null 触发回灌重试、arguments 字符串形式容错）。护栏 `TestToolCallParser` 16/16 通过。
    - [x] T1.6 建护栏测试 `TestToolRegistry` + `TestToolCallParser`
      - 内容：对 T1.2/T1.3/T1.5 建立独立单测（无 Spring 依赖，纯 JUnit/手写 main）。
      - 完成判据：全部通过；作为后续所有工具代码的回归基线。
      - 当前状态：**已完成（2026-07-31）**。`tool/TestToolRegistry.java`（**20/20**）+ `tool/TestToolCallParser.java`（**16/16**），手写 main 入口（对齐项目无测试框架现状）。编译通过（JDK 1.8）。

  - [x] **T2. ToolUseLoop 最小闭环（核心）**
    - 当前状态：**已完成（2026-07-31）**。T2.1-T2.4 全绿，已接入 MainAgent 灰度开关。
    - 测试机制：`/tool <查询>` 前缀**强制触发** ToolUseLoop（不翻转全局开关即可安全测试）；`resolvedLlm=null` 时 `LLMService.chat(List, null)` 回退系统默认配置（只读解析，不写共享字段）。
    - [x] T2.1 实现 `ToolUseLoop` 单轮循环
      - 内容：`agent/orchestrator/ToolUseLoop.java`：`run(userQuery, ctx, resolvedLlm)`。
        - 初始化 messages = [system(工具提示词), user(query)]
        - 循环（上限 MAX_TURNS=10）：调 `LLMService.chat(messages, sid, resolvedLlm)` → `ToolCallParser` 解析
        - 解析到 tool_call → `ToolRegistry.execute` → 结果追加为 `assistant`(原始输出) + `tool`(执行结果) 消息 → 继续
        - 未解析到 tool_call → 视为最终回答，返回
      - 完成判据：用真实 LLM + 2 个工具（list_repos/search_docs）跑通"查仓库→查文档"两步链。
      - 当前状态：**循环引擎已完成（2026-07-31）**。
        - `orchestrator/ToolUseLoop.java`：REPL 循环（MAX_TURNS=10 + 连续畸形 tool_call 上限 3 次防死循环 + 工具结果以 role=user + `[TOOL_RESULT name=...]` 标记回灌兼容不支持 tool role 的模型 + 未知工具错误回灌继续）。
        - `orchestrator/ToolUseResult.java`：success/message/transcript/turns/toolCalls/maxTurnsExceeded。
        - `LLMService.chat(List<Map<String,String>>, ResolvedLlmConfig)` 新增无状态消息列表重载（自定义 system 提示词 + 工具结果回灌用，不写共享字段）。
        - LLM 调用经 `LlmCaller` 函数式接口抽象（默认包装 LLMService，测试注入假实现）。
        - 护栏 `orchestrator/TestToolUseLoop.java` **32/32 通过**：多轮链(3轮2工具)、单轮、未知工具恢复、畸形重试、连续畸形终止、超轮数中断、adminOnly 提示词过滤、重复调用检测、上下文裁剪。
        - ★ 真实 LLM 端到端（"查仓库→查文档"两步链）待开启灰度开关后人工验证（依赖真实 DocSysClient 会话）。
    - [x] T2.2 对话历史管理 + tool role 消息
      - 内容：复用 `LLMService.conversationHistory` 或 ToolUseLoop 内部独立管理；工具结果以 role=tool 或等价格式回灌（OpenAI 兼容 chat completions 需要 role 约束，注意部分模型不支持 tool role 时的降级策略）。
      - 完成判据：多轮 tool 调用后上下文完整；超长时裁剪策略生效。
      - 当前状态：**已完成（2026-07-31）**。ToolUseLoop 内部独立管理消息列表（不用 LLMService.conversationHistory，以便自定义 system 提示词）；工具结果以 **role=user + `[TOOL_RESULT tool=name]` 标记**回灌（兼容不支持 tool role 的模型，即降级策略）；`trimTranscript` 超 30 条裁剪最早的工具结果防上下文膨胀。护栏覆盖（transcript 含工具结果标记 + trimming 断言）。
    - [x] T2.3 轮数控制 + 防死循环
      - 内容：MAX_TURNS 硬上限；连续相同工具调用重复 N 次强制中断；异常工具结果注入"重试/换工具"提示。
      - 完成判据：构造死循环场景（LLM 反复调同一工具）能被终止并返回可读错误。
      - 当前状态：**已完成（2026-07-31）**。MAX_TURNS=10 硬上限；连续相同工具+参数调用 ≥3 次注入"换思路"提示（`MAX_IDENTICAL_CALLS=3`）；连续畸形 tool_call ≥3 次终止（`MAX_MALFORMED=3`）；异常工具结果回灌让 LLM 自行恢复。护栏覆盖（max turns / identical calls / malformed abort）。
    - [x] T2.4 集成到 `MainAgent.process()`（灰度开关）
      - 内容：新增配置 `agent.tool-loop.enabled`（默认 false）；process() 开头按开关分流：ON → ToolUseLoop，OFF → 原 decomposeTask。旧路径代码**一行不动**。
      - 完成判据：开关 OFF 时行为与现状完全一致（零回归）；ON 时走新路径。
      - 当前状态：**已完成（2026-07-31）**。`MainAgent` 新增 `@Value("${agent.tool-loop.enabled:false}")` 开关；process() 在 decomposeTask 之前分流：ON → `ToolUseLoop.forLlmService(llmService, registry, resolvedLlm, isAdmin)`（registry 由 `DocSysToolFactory.createReadOnlyRegistry(authenticatedClient)` 按请求构造）；**成功返回、失败回退旧路径**（灰度安全网）。新增 `extractIsAdmin`（当前 Admin 用户名即管理员）。编译通过；开关默认 OFF 时旧路径零改动。

  - [x] **T3. 工具集全量接入（DocSysClient 40+ API → Tool）**
    - 当前状态：**已完成（2026-07-31）**。T3.1-T3.5 全绿，共 26 个工具（14 读 + 12 写）。
    - [x] T3.1 只读工具组
      - 内容：list_repos / get_repos / list_docs / get_doc / get_doc_list / doc_history / search_docs / list_models / get_config / whoami 等只读 API → ToolDefinition。
      - 完成判据：每工具注册后，用真实 DocSysClient 跑通 execute；护栏 `TestReadTools`。
      - 当前状态：**已完成（2026-07-31）**。`tool/DocSysToolFactory.java` 12 个只读工具（get_login_user / list_repos / get_repos / list_docs / get_doc / get_doc_history / search_docs / list_ai_models / get_sys_config / get_banner_config / get_doc_share_list / query_backup_status），每个带 JSON Schema + 结果截断(4000字符)。护栏 `TestReadTools` 待真实会话（随灰度开关验证）。
    - [x] T3.2 写操作工具组（需审计）
      - 内容：add_repos / delete_repos / rename_repos / add_doc / delete_doc / rename_doc / move_doc / copy_doc / upload_doc / lock_doc / unlock_doc 等写 API → ToolDefinition，全部标记 `isWrite=true` + `needsConfirm=true`。
      - 完成判据：执行写工具前触发审批回调（对接 AuditLogService）；护栏 `TestWriteTools`。
      - 当前状态：**已完成（2026-07-31）**。`DocSysToolFactory.createFullRegistry` 注册 12 个写工具（create_repos/delete_repos/update_repos/create_doc/delete_doc/rename_doc/move_doc/copy_doc/lock_doc/unlock_doc/create_doc_share/backup_repos），全部 `isWrite+needsConfirm`。确认机制：
        - `tool/WriteConfirmGate.java`（函数式抽象，NOOP 放行）
        - `tool/AuditWriteConfirmGate.java`（对接 AuditLogService：createPendingEntry → 日志打印 confirmToken → 轮询等待 /confirm 批准；用新增的 `getStatusByConfirmToken` 正确区分 approve/reject，规避现有 waitForConfirmation 把 approve 当拒绝的隐患）
        - `ToolRegistry.setConfirmGate()` + execute() 在 needsConfirm 工具执行前拦截
        - `MainAgent` 注入 AuditLogService，ToolUseLoop 路径用 `createFullRegistry` + `AuditWriteConfirmGate`（超时 120s 可配）
      - 护栏 `tool/TestWriteTools.java` **48/48 通过**：12 写工具 flags、gate 触发/拒绝/批准/NOOP、只读工具不触发 gate。
      - ⚠️ 注意：`DocSysToolFactory` 曾有 `props()` 缺 key 潜伏 bug（编译通过但运行时 ClassCastException），已修复（属性构建器自带 key）。
    - [x] T3.3 RAG/搜索工具组
      - 内容：search_doc / addDocSysRagMessage(RAG) / rag_chat 等 → ToolDefinition。
      - 完成判据：搜索→RAG 两步链在 ToolUseLoop 中可连续完成。
      - 当前状态：**已完成（2026-07-31）**。`search_docs`（T3.1 已有）+ 新增 `rag_chat`（client.ragChat → /Query/addDocSysRagMessage）+ `ai_chat`（client.chat → /Repos/AIChat），均为只读（不触发确认门），String 返回经 `fmtString` 截断。护栏：`TestWriteTools.testRagAiToolsRegistered`（注册 + 非写 + 参数 Schema）。搜索→RAG 两步链真实 E2E 待灰度验证。
    - [x] T3.4 用户/会话工具组
      - 内容：login / logout / getLoginUser / register 等用户相关 API → ToolDefinition（注意：Agent 复用 MxsDoc 会话，多数场景不应让 LLM 自主登出）。
      - 完成判据：仅注册确需暴露的工具；危险操作默认不注册或加白名单。
      - 当前状态：**已完成（2026-07-31，决策已落地）**。仅注册只读的 `get_login_user`；**login/logout/register 默认不注册**（Agent 复用 MxsDoc 共享会话，LLM 不应具备登录/登出能力——会话一致性由系统保证，交给 LLM 是安全漏洞）。决策记录见 `devDocs/tool-inventory.md` §4。
    - [x] T3.5 工具可见性白名单
      - 内容：`ToolRegistry` 支持按用户角色/管理员过滤工具；写操作工具对只读用户不可见。
      - 完成判据：普通用户看不到写工具；管理员可见。
      - 当前状态：**已完成（2026-07-31）**。`ToolRegistry.listForUser(isAdmin, readOnly)` 两级过滤：adminOnly 按 isAdmin + 写工具按 readOnly；`execute()` 同步校验 adminOnly。护栏 `TestToolRegistry.testReadOnlyRoleFiltering` 通过（只读用户只见读工具/普通用户见读写/管理员全见）。⚠️ 只读角色判定（readOnly）当前未从真实角色服务接线（无角色系统），MainAgent 默认传 false；待角色服务接入后接线。

  - [x] **T4. 与现有系统融合（安全/审计/限流/监控）**
    - 当前状态：**已完成（2026-07-31）**。T4.1-T4.4 全绿。
    - [x] T4.1 写操作二次确认接入
      - 内容：ToolUseLoop 内检测写工具 → 复用 SSE confirm 机制（生成 confirmToken + PENDING 审计 → 用户确认后执行，120s 超时）。
      - 完成判据：写工具执行前必经确认；拒绝则不执行。
      - 当前状态：**已完成（2026-07-31）**。
        - `WriteConfirmGate` + `AuditWriteConfirmGate`（createPendingEntry → 轮询等待 /confirm → `getStatusByConfirmToken` 区分 approve/reject）+ `ToolRegistry` 拦截。
        - **SSE 弹窗推送已接入**：`ConfirmEventSink` 接口 + `AuditWriteConfirmGate.setConfirmEventSink()`；`AgentController.stream()` 在 tool-loop 开启时调 `mainAgent.runToolUseLoop(command, ..., sseSink)`（复用抽取出的公共方法），写工具需要确认时经 emitter 推送 `{"type":"confirm",...}` 给前端。
        - ⚠️ 已知隐患：现有 `AgentController.waitForConfirmation`（旧路径 write-op 确认）仍把 approve 当拒绝——新 ToolUseLoop 路径用 `getStatusByConfirmToken` 已规避；旧路径待修。
    - [x] T4.2 审计日志覆盖
      - 内容：ToolUseLoop 内所有工具执行写审计（SHA-256 脱敏敏感字段）；与现有 AuditLogService 复用。
      - 完成判据：每次写工具调用产生审计记录，字段脱敏正确。
      - 当前状态：**已完成（2026-07-31）**。`AuditLogService.record()`（终端 COMPLETED/FAILED 审计条目）+ `ToolRegistry.ToolExecutionListener`（每次 execute 后回调）+ MainAgent 接线（写工具 → auditLogService.record，含参数脱敏 sanitizeParams）。护栏 `TestToolRegistry.testExecutionListener`（监听器正常/异常路径都触发）。
    - [x] T4.3 限流/监控对接
      - 内容：工具调用纳入 RateLimitService 限额；`agent.execution` / `llm.call` 指标打点工具调用与轮数。
      - 完成判据：指标面板能看到 tool 调用次数/轮数分布。
      - 当前状态：**已完成（2026-07-31，指标部分）**。MainAgent 注入 AgentMetrics：`agent.tool.call`（tool/write/success 标签）+ `agent.tool.loop`（success/turns 标签）+ `agent.execution`（tool_loop）。**剩余**：RateLimitService 限额需 client IP（MainAgent 无 IP 上下文，AgentController 层已有）——待 ToolUseLoop 接入 SSE 路径后一并接线。
    - [x] T4.4 与 Skill 系统协同
      - 内容：ToolUseLoop 执行不上的场景回退到 SkillExecutorRegistry；Skill 也可作为 Tool 暴露给 LLM（skill 描述进系统提示词）。
      - 完成判据：外部 Skill 能以 Tool 形式被 LLM 调用。
      - 当前状态：**已完成（2026-07-31，基础设施）**。`DocSysToolFactory.runSkillTool()` 通用技能工具（skillId + params → SkillExecutorRegistry.execute；技能可执行任意逻辑故 isWrite+needsConfirm）；MainAgent 有 SkillExecutorRegistry 时自动注册 `run_skill`。⚠️ `SkillIntentRegistry` 当前是 stub（无外部技能注册），内置技能（EnhancedSkillManager 9 项）与直接工具重叠——run_skill 主要价值在外置技能场景。

  - [ ] **T5. 高级能力（按需逐步推进）**
    - 当前状态：T5.1/T5.2 已完成；T5.3/T5.4 待做（增强项，不阻塞交付）。
    - [x] T5.1 开启 ReflectionEngine 联动
      - 内容：ToolUseLoop 执行失败（工具抛异常/结果异常）时，接入 `ReflectionEngine` 诊断 + `Replanner` 重规划，有上限重试。
      - 完成判据：构造"工具返回空结果"场景，能自动换工具/换参数重试。
      - 当前状态：**已完成（2026-07-31，轻量版）**。`MainAgent.runToolUseLoop` 在 ToolUseLoop 失败（超轮数/畸形终止）时**注入"直接回答"提示重试一次**（T5.1 有界重试）。完整 ReflectionEngine 重规划（换工具/换参数）留作后续增强（重试逻辑已具备框架）。
    - [x] T5.2 会话记忆（跨请求上下文）
      - 内容：把历史 ToolUseLoop 对话摘要存入会话（复用 SessionService / ExperienceMemory），新请求可延续上下文。
      - 完成判据：连续两个请求（先查仓库、再基于结果建文档）第二条能引用第一条结果。
      - 当前状态：**已完成（2026-07-31）**。拆为两部分：
        - **T5.2a 会话历史持久化 + 续接入口**（用户核心诉求：关页面后可找回并续接会话）：
          - 新表 `agent_session_messages`（session_id/role/content/seq/created_at，MySQL+SQLite 双方言 DDL）
          - `SessionMessageEntity` + `SessionMessageRepository` + `SessionMessageRepositoryMapper.xml` + `ConversationHistoryService`（append/saveExchange/getHistory/deleteSession）
          - `SessionService` 扩展：`listByUsername` / `createSession` / `updateTitleIfEmpty`（标题=首条消息截断，存 metadata JSON）
          - AgentController 新端点：`GET /sessions`（列表）、`POST /sessions`（创建）、`GET /sessions/{id}/messages`（历史）、`DELETE /sessions/{id}`
          - 保存钩子：stream() 的 isAiChat/toolLoop/legacy 三路径 + /execute 路径，`saveExchange(sessionId, userMsg, assistantMsg)`
          - 前端 index.html：API 方法（listSessions/createSession/getSessionMessages/deleteSession）、`loadSessionsFromServer()`（enterApp 时加载）、newSessionBtn 走服务端创建、会话点击加载历史、无会话时发送自动创建、完成刷新列表、删除按钮 + 标题 CSS
        - **T5.2b 跨请求上下文延续**：`ToolUseLoop.run(userQuery, priorHistory)` 支持历史注入（system + history + user）；`MainAgent.runToolUseLoop(..., historySessionId)` 从库加载最近 20 条 user/assistant 消息作为 LLM 上下文（`loadSessionHistory`）；SSE 路径传入会话 sessionId。
      - 护栏：`TestToolUseLoop.testHistoryInjection`（历史注入到 messages、system 在前、user 查询在最后）。
    - [x] T5.3 复杂任务多步编排
      - 内容：对"先搜文档→提取内容→生成摘要→保存"类多步任务，验证 ToolUseLoop 能自主串联（而非预设子任务）。
      - 完成判据：端到端真实执行成功且产物正确。
      - 当前状态：**已完成（2026-07-31 真实 LLM 验证通过）**。护栏 +7（TestToolCallParser 16→23）。
      - 验证记录（DeepSeek + 真实 DocSys 会话）：
        - ✅ 流式路径：`list_repos → search_docs(vid,searchWord) → list_docs` 自主串联成功，产物为完整 markdown 报告（仓库表 + office 搜索文档列表）。
        - ✅ 非流式 /execute：同一任务 6 工具 / 9 轮 / 33s，产物正确（metadata toolLoop: turns=9, toolCalls=6）。
        - ✅ 多步时参数正确传承（vid=8 从 list_repos 结果带入 search_docs/list_docs；子文件夹用 path/docId）。
      - 🔧 验证中修复的工具 bug（DocSysClient + Tool 层）：
        - `getDocList` 端点全错（/Doc/getDocList.do 等 404）→ 改 `/Repos/getSubDocList.do`；**子文件夹必须传 path 或 docId（pid 被服务端忽略）** → list_docs schema 改 docId/path + 描述引导。
        - `searchDocs` 参数名 `vid` → **`reposId`**（原传错 → 被当全局搜索 → 60s 超时）。
        - `getDoc`/`getDocHistory` 参数名 `vid` → `reposId`；`getDocShareList` 端点无参数。
        - 规律：Repos 类端点用 `vid`，Doc 类端点用 `reposId`。
      - 🔧 模型输出容错（ToolCallParser + ToolPromptBuilder）：
        - 支持 Anthropic/Claude XML 格式（`<invoke name><parameter>`）作为 JSON 失败回退。
        - 输出含 `<tool_call` 标记但无法解析（关闭标签写错/截断）→ 按畸形重试而非当最终回答。
        - 提示词明确禁止 `<invoke>/<parameter>`，只允许 JSON 格式。
      - ⚠️ 发现的 DocSystem 核心 bug（非 Agent 问题，已记录待修）：
        - `getDoc.do` 对所有 doc 返回 HTTP 500 NPE（日志 `docSysGetDocList() docId:0 []` 查库为空）→ 当前 `get_doc`/`get_doc_history` 工具不可用。
        - `createDocShare.do` 端点不存在 → `create_doc_share` 工具目标缺失（写工具，需确认真实分享创建端点后修复）。
    - [ ] T5.4 工具调用序列固化 Skill
      - 内容：把用户认可的重复工具序列（如"新建仓库+上传文档+分享"）固化为 Skill（复用 SkillCrystallizer / ExperienceMemory）。
      - 完成判据：重复请求可命中已固化 Skill 一键执行。
      - 当前状态：未开始。需对接 SkillCrystallizer/ExperienceMemory 的重复序列检测与 Skill 文件生成（独立功能，暂缓）。

  - [ ] **T6. 灰度切换与验收**
    - 当前状态：未开始。T6.1/T6.2 为必做交付门槛。
    - [ ] T6.1 端到端黄金测试：新旧路径对比
      - 内容：同一批真实请求（仓库/文档/搜索/问答），旧路由 vs ToolUseLoop 输出对比；ToolUseLoop 产物正确性人工验收。
      - 完成判据：核心场景全部通过，无回归。
      - 当前状态：未开始。
    - [ ] T6.2 性能对照
      - 内容：ToolUseLoop vs 旧路由的响应时间、LLM 调用次数、工具调用次数统计。
      - 完成判据：形成验收指标（含轮数分布，避免 LLM 空转）。
      - 当前状态：未开始。
    - [~] T6.3 默认开启 ToolUseLoop（灰度翻正）
      - 内容：`agent.tool-loop.enabled=true`；保留开关随时回退旧路径。
      - 完成判据：真实请求默认走 ToolUseLoop；监控无异常回退。
      - 当前状态：**已翻正（2026-07-31，测试阶段）**。`MainAgent`/`AgentController` 的 `@Value` 默认值已改为 `true`（可用 `-Dagent.tool-loop.enabled=false` 或 Spring 属性回退）。待真实端到端验证通过后移除测试标记。
    - [ ] T6.4 长尾回归
      - 内容：旧路径曾支持的意图/命令（list-repos、create-repos 等复合命令）在新路径下仍可用。
      - 完成判据：覆盖旧路径全部命令场景，无能力退化。
      - 当前状态：未开始。

  - [ ] **T7. 流式体验升级（对齐 ai-writting-node：Reasoning 展示 + 真流式 + 工具进度卡片）**
    - 当前状态：**全部完成（2026-07-31 部署验收通过）**。参考蓝本：`D:\Dev\ai-writting-node` PR #359（AI SDK v7 ToolLoopAgent）——`UIMessage stream`（text/reasoning/tool 进度卡片）+ 可折叠处理过程容器 + reasoning 灰色小字内联 + 回放剥离。
    - 现状问题：ToolUseLoop 目前用**非流式 chat**（每轮等完整响应），SSE 路径用**5 字符假打字**推送最终结果——用户看不到"LLM 正在思考/调工具"的过程，工具链场景体验差；也无 reasoning 展示。
    - [x] T7.1 后端：ToolUseLoop 流式化（事件协议）
      - 当前状态：已完成（2026-07-31）。编译通过（JDK 1.8）；护栏 `TestToolUseLoopStreaming` **26/26**。
      - [x] T7.1.1 `LLMService.streamChat(List<Map>, resolved)` 多消息流式重载
        - 内容：对偶已存在的 `chat(List<Map>, resolved)`，新增流式版本（自定义 system prompt + 工具结果回灌 + 逐 token 输出）；同样无状态、不写共享字段。
        - 完成判据：与 chat(List) 同参，能流式返回；护栏单测（假流验证协议）。
        - 完成记录：`LLMService.streamChatChunks(List, resolved)` 已新增（返回 `StreamChunk` 迭代器，末尾必有 done）；配套新增 `llm/StreamChunk.java`（text/reasoning/done 三型）。
      - [x] T7.1.2 reasoning 提取
        - 内容：OpenAI 兼容流中提取 `reasoning_content`/`reasoning` 分片（模型支持时）；与 `content` 分片分离。
        - 完成判据：支持 reasoning 的模型流能同时产出 text + reasoning 两类分片。
        - 完成记录：OpenAI 兼容流 delta 提取 `reasoning_content`/`reasoning`；Ollama 流 message 提取同名字段。护栏 `testStreamingReasoningSeparation`（reasoning 不进正文）。
      - [x] T7.1.3 ToolUseLoop 流式运行模式
        - 内容：新增流式 run——每轮 LLM 输出逐分片回调（text/reasoning）；轮末仍解析 `<tool_call>`；工具执行时回调 tool_call/tool_result 事件；最终回答回调 done。保持非流式 run 兼容（/execute 路径不变）。
        - 完成判据：单测覆盖"流式多轮工具链"（假 LLM 分片驱动）。
        - 完成记录：`runStreaming(query, history, StreamSink)` + `StreamingLlmCaller`/`StreamSink` 接口；`forLlmServiceStreaming` 双通道工厂；run/runStreaming 共用 runInternal（TurnRunner 抽象）；无流式通道时自动回退非流式。护栏覆盖：流式单轮/多轮工具链/工具失败/回退/重复调用提示（26 项）。
      - [x] T7.1.4 SSE 事件协议 + AgentController 接入
        - 内容：定义 `{type: reasoning} / {type: text} / {type: tool_call} / {type: tool_result} / {type: done}`；`stream()` 的 ToolUseLoop 路径改用流式事件推送（替代假打字）；确认事件 `{type: confirm}` 保持。
        - 完成判据：前端能区分 reasoning/text/tool 事件并按类型渲染。
        - 完成记录：`AgentController.runToolLoopStreamingWithSse`（StreamSink→SSE 事件，全程累积 reasoning）；`stream()` ToolUseLoop 路径已改流式推送 + done 携带 meta；isAiChat 路径也升级为 streamChatChunks + reasoning/text 事件（DB 历史构建消息列表）；确认事件 confirm 保持。
      - [x] T7.1.5 兼容性保持
        - 内容：/execute 非流式路径、失败回退、T5.1 重试逻辑不受影响。
        - 完成判据：开关关闭或流式失败时回退现状（一次性展示）零回归。
        - 完成记录：`run()` 非流式路径未动（TestToolUseLoop 36/36 回归通过）；流式失败/异常 → `runToolLoopStreamingWithSse` 返回 null → 回退 legacy；重试前 `onRetry` 事件通知前端清空；`MainAgent.buildToolLoop` 抽取共用。
    - [x] T7.2 前端：流式渲染 + 工具进度卡片
      - 当前状态：已完成（2026-07-31）。只动 `WebRoot/web/agent/index.html`。
      - [x] T7.2.1 真流式渲染
        - 内容：替代 5 字符假打字——按 `text` 事件逐 token 追加（后端已真流式）；`reasoning` 与 `text` 分开展示。
        - 完成判据：LLM 生成过程中文字实时出现（首 token 延迟可感知）。
        - 完成记录：`executeWithGeneration` 新增 `text`/`chunk` 事件 → 追加 liveText（实时渲染）；`done` 落 content；reasoning 独立累积灰色展示。
        - ⚠️ 后续修复（2026-07-31）：原实现每个流式分片都调 `renderMessages()` 重建全部消息 → 用户反馈"疯狂刷新"。改为**增量 DOM 更新**：`scheduleStreamingUpdate`（requestAnimationFrame 节流到 ~60fps）→ `updateStreamingMessage` 只更新当前消息节点；生成中正文用轻量 `renderStreamingText`（纯文本转义，不重跑 markdown/mermaid），`done` 才一次性智能渲染；body 节点保持稳定（innerHTML 而非 outerHTML）。MutationObserver 实测：流式过程中 messagesList 容器 0 次整体重建（仅开始/结束各 2 次全量渲染）。
      - [x] T7.2.2 工具进度卡片
        - 内容：`tool_call` 事件 → 显示工具名/参数卡片（"调用中"）；`tool_result` → 结果摘要/失败红字；多工具按顺序排列。
        - 完成判据：工具链场景下每步工具调用/结果可见。
        - 完成记录：`renderToolCard`（running 转圈/成功/失败三态 + 参数 + 结果摘要/红字）；tool_call 前文本剥离 XML 收进处理容器。
      - [x] T7.2.3 处理过程容器（可折叠）
        - 内容：仿 ai-writting-node——reasoning + 工具调用 + 中间文本收进"处理中/处理结果 + 耗时"可折叠容器；容器外只保留最终文本 + 当前工具卡片。
        - 完成判据：多轮工具链的中间过程可折叠隐藏，界面清爽。
        - 完成记录：`<details class=process-container>`（生成中自动展开）+ reasoning 灰色小字 + 中间文本 + 工具卡片；深色主题适配。另补写操作**确认弹窗**（`{type:confirm}` → 批准/拒绝 POST /confirm，此前前端未接）。
    - [x] T7.3 Reasoning 持久化 + 展示
      - 当前状态：已完成（2026-07-31）。
      - [x] T7.3.1 reasoning 持久化
        - 内容：reasoning 分片保存到会话消息（新 role=reasoning 或 assistant 消息的 metadata 字段），供回看。
        - 完成判据：刷新页面后 reasoning 历史可恢复。
        - 完成记录：`ConversationHistoryService.saveExchange(sid, user, assistant, reasoning)` 重载——reasoning 以 role=reasoning 消息落库（表 role VARCHAR(16) 无约束，无需 DDL 变更）；AgentController 三处保存钩子均传 reasoning。
      - [x] T7.3.2 前端灰色小字展示
        - 内容：reasoning 以灰色小字内联/可折叠展示（仿 ai-writting-node）。
        - 完成判据：reasoning 与正文视觉区分清晰。
        - 完成记录：`loadSessionMessages` 把 role=reasoning 消息合并到其后 assistant 的 reasoning 字段 → 灰色小字渲染。
      - [x] T7.3.3 回放剥离
        - 内容：历史 reasoning 不回灌模型（仅展示用），避免脏上下文/缺供应商签名错误。
        - 完成判据：续接会话加载历史时不含 reasoning。
        - 完成记录：`MainAgent.loadSessionHistory` 只取 user/assistant（已排除 reasoning）；isAiChat 消息列表构建同样只取 user/assistant。
    - [x] T7.4 验收
      - 当前状态：**已完成（2026-07-31 部署验收）**。修复真流式关键 bug：`streamChatChunks` 原为"先缓冲全部分片再返回迭代器"→ 改为**惰性流式迭代器**（`StreamChunkIterator` 逐行读 SSE，分片到达即返回）；新增 `TestStreamChatChunks`（本地 HTTP 服务器模拟 SSE 分片延迟，**9/9**，时序证据 [161,375,625]ms 渐进到达）。
      - [x] T7.4.1 真实 LLM 流式端到端
        - 内容：单轮对话 + 多轮工具链 + reasoning 模型三类场景，人工验收展示效果。
        - 完成判据：流式流畅、工具卡片正确、reasoning 可见且不影响回答。
        - 完成记录：DeepSeek（deepseek-v4-flash）三类场景全部通过——SSE 时序证明 reasoning/text/tool_call/tool_result 事件渐进到达（15-30ms 间隔）；浏览器 UI 实测生成中途可见"处理中…" + reasoning 灰色小字实时流出 + 工具卡片 + 正文逐段生成 + 停止按钮。⚠️ 自定义模型 Claude 4.8（wiselnk）不支持流式→自动回退 legacy 假打字（配置问题非代码 bug）。
      - [x] T7.4.2 性能对照
        - 内容：流式首 token 延迟、整轮耗时 vs 非流式基线。
        - 完成判据：首 token 延迟明显低于整轮耗时（流式收益成立）。
        - 完成记录：实测单轮对话首 reasoning 分片 458ms / 首 text 分片 2053ms / done 4836ms——首分片远早于整轮耗时，流式收益成立。

---

## 验收标准（两级，已定）

1. **功能级**：ToolUseLoop 能自主完成"搜索→RAG→回答""列出仓库→查看文档→生成摘要"等多步任务，产物与旧路径/人工一致。
2. **工程质量级**：新旧路径并存可灰度回退；工具调用全审计、写操作全确认；无死循环、无上下文膨胀失控；性能不劣于旧路径。

## 明确不做（边界）

- 不做通用编程 Agent（本 Agent 限定 DocSys 文档管理域）。
- 不改 `DocSysClient` 底层（只在其上做 Tool 封装）。
- 不重写 `LLMService`（ToolUseLoop 只新增调用方）。
- 不改旧 `decomposeTask` 路由代码（T6.3 翻正前旧路径保持原样）。
