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

---

## 任务树（唯一入口）

- [ ] **T0. 完成 DocSys Agent 的工具推理化改造（ToolUseLoop），默认开启并替代单轮规则路由**
  - 当前状态：未开始。基础设施盘点、Tool 模型、ToolUseLoop、工具集接入、系统融合、验收均未落地。

  - [ ] **T1. 立骨架与护栏（先于任何功能）**
    - 当前状态：未开始。先有"可对比、可回退"的地基，再动业务逻辑。
    - [ ] T1.1 现状盘点：DocSysClient 全部 API → 候选 Tool 清单
      - 内容：逐方法列出 `DocSysClient` 的 40+ API（方法名、入参、出参、读/写性质），产出"工具清单"作为 Tool 定义依据。
      - 完成判据：清单覆盖全部 API，每项标注 读/写/需审计。
      - 当前状态：未开始。清单落 `devDocs/tool-inventory.md`。
    - [ ] T1.2 建 Tool 定义模型 `ToolDefinition`
      - 内容：`agent/tool/ToolDefinition.java`：`name / description / parameters(JSON Schema) / executor(函数式) / isWrite(写操作标记) / needsConfirm`。
      - 完成判据：可构造一个最小工具（如 list_repos）并通过单测。
      - 当前状态：未开始。
    - [ ] T1.3 建 Tool 注册表 `ToolRegistry`
      - 内容：`agent/tool/ToolRegistry.java`：`register(ToolDefinition)` / `list()` / `find(name)` / `execute(name, args)`；执行时解析 JSON 参数。
      - 完成判据：注册 3 个只读工具，execute 返回正确结果；未知工具抛清晰异常。
      - 当前状态：未开始。
    - [ ] T1.4 建系统提示词模板（工具使用描述）
      - 内容：`agent/tool/ToolPromptBuilder.java`：把 ToolRegistry 里的工具描述渲染成 system prompt（工具名/描述/参数 Schema）；描述工作方式（可连续调用、用 `<tool_call>` 标记、最终回答）。
      - 完成判据：模板能渲染 N 个工具；与 LLM 兼容（不超 token 预算，必要时截断）。
      - 当前状态：未开始。
    - [ ] T1.5 建工具调用解析器 `ToolCallParser`
      - 内容：解析 LLM 输出中的 `<tool_call>{"name":"...","arguments":{...}}</tool_call>`；支持失败容错（JSON 不完整时返回解析错误给 LLM 重试）。
      - 完成判据：正确/畸形/缺失工具名三类输入的单测通过。
      - 当前状态：未开始。
    - [ ] T1.6 建护栏测试 `TestToolRegistry` + `TestToolCallParser`
      - 内容：对 T1.2/T1.3/T1.5 建立独立单测（无 Spring 依赖，纯 JUnit/手写 main）。
      - 完成判据：全部通过；作为后续所有工具代码的回归基线。
      - 当前状态：未开始。

  - [ ] **T2. ToolUseLoop 最小闭环（核心）**
    - 当前状态：未开始。
    - [ ] T2.1 实现 `ToolUseLoop` 单轮循环
      - 内容：`agent/orchestrator/ToolUseLoop.java`：`run(userQuery, ctx, resolvedLlm)`。
        - 初始化 messages = [system(工具提示词), user(query)]
        - 循环（上限 MAX_TURNS=10）：调 `LLMService.chat(messages, sid, resolvedLlm)` → `ToolCallParser` 解析
        - 解析到 tool_call → `ToolRegistry.execute` → 结果追加为 `assistant`(原始输出) + `tool`(执行结果) 消息 → 继续
        - 未解析到 tool_call → 视为最终回答，返回
      - 完成判据：用真实 LLM + 2 个工具（list_repos/search_docs）跑通"查仓库→查文档"两步链。
      - 当前状态：未开始。★ [UNVALIDATED] 无端到端验证。
    - [ ] T2.2 对话历史管理 + tool role 消息
      - 内容：复用 `LLMService.conversationHistory` 或 ToolUseLoop 内部独立管理；工具结果以 role=tool 或等价格式回灌（OpenAI 兼容 chat completions 需要 role 约束，注意部分模型不支持 tool role 时的降级策略）。
      - 完成判据：多轮 tool 调用后上下文完整；超长时裁剪策略生效。
      - 当前状态：未开始。
    - [ ] T2.3 轮数控制 + 防死循环
      - 内容：MAX_TURNS 硬上限；连续相同工具调用重复 N 次强制中断；异常工具结果注入"重试/换工具"提示。
      - 完成判据：构造死循环场景（LLM 反复调同一工具）能被终止并返回可读错误。
      - 当前状态：未开始。
    - [ ] T2.4 集成到 `MainAgent.process()`（灰度开关）
      - 内容：新增配置 `agent.tool-loop.enabled`（默认 false）；process() 开头按开关分流：ON → ToolUseLoop，OFF → 原 decomposeTask。旧路径代码**一行不动**。
      - 完成判据：开关 OFF 时行为与现状完全一致（零回归）；ON 时走新路径。
      - 当前状态：未开始。

  - [ ] **T3. 工具集全量接入（DocSysClient 40+ API → Tool）**
    - 当前状态：未开始。按 读→写→RAG→用户 分组逐步接入，每组接入后立即用护栏验证。
    - [ ] T3.1 只读工具组
      - 内容：list_repos / get_repos / list_docs / get_doc / get_doc_list / doc_history / search_docs / list_models / get_config / whoami 等只读 API → ToolDefinition。
      - 完成判据：每工具注册后，用真实 DocSysClient 跑通 execute；护栏 `TestReadTools`。
      - 当前状态：未开始。★ [UNVALIDATED] 无端到端验证。
    - [ ] T3.2 写操作工具组（需审计）
      - 内容：add_repos / delete_repos / rename_repos / add_doc / delete_doc / rename_doc / move_doc / copy_doc / upload_doc / lock_doc / unlock_doc 等写 API → ToolDefinition，全部标记 `isWrite=true` + `needsConfirm=true`。
      - 完成判据：执行写工具前触发审批回调（对接 AuditLogService）；护栏 `TestWriteTools`。
      - 当前状态：未开始。★ [UNVALIDATED] 无端到端验证。
    - [ ] T3.3 RAG/搜索工具组
      - 内容：search_doc / addDocSysRagMessage(RAG) / rag_chat 等 → ToolDefinition。
      - 完成判据：搜索→RAG 两步链在 ToolUseLoop 中可连续完成。
      - 当前状态：未开始。★ [UNVALIDATED] 无端到端验证。
    - [ ] T3.4 用户/会话工具组
      - 内容：login / logout / getLoginUser / register 等用户相关 API → ToolDefinition（注意：Agent 复用 MxsDoc 会话，多数场景不应让 LLM 自主登出）。
      - 完成判据：仅注册确需暴露的工具；危险操作默认不注册或加白名单。
      - 当前状态：未开始。
    - [ ] T3.5 工具可见性白名单
      - 内容：`ToolRegistry` 支持按用户角色/管理员过滤工具；写操作工具对只读用户不可见。
      - 完成判据：普通用户看不到写工具；管理员可见。
      - 当前状态：未开始。

  - [ ] **T4. 与现有系统融合（安全/审计/限流/监控）**
    - 当前状态：未开始。
    - [ ] T4.1 写操作二次确认接入
      - 内容：ToolUseLoop 内检测写工具 → 复用 SSE confirm 机制（生成 confirmToken + PENDING 审计 → 用户确认后执行，120s 超时）。
      - 完成判据：写工具执行前必经确认；拒绝则不执行。
      - 当前状态：未开始。
    - [ ] T4.2 审计日志覆盖
      - 内容：ToolUseLoop 内所有工具执行写审计（SHA-256 脱敏敏感字段）；与现有 AuditLogService 复用。
      - 完成判据：每次写工具调用产生审计记录，字段脱敏正确。
      - 当前状态：未开始。
    - [ ] T4.3 限流/监控对接
      - 内容：工具调用纳入 RateLimitService 限额；`agent.execution` / `llm.call` 指标打点工具调用与轮数。
      - 完成判据：指标面板能看到 tool 调用次数/轮数分布。
      - 当前状态：未开始。
    - [ ] T4.4 与 Skill 系统协同
      - 内容：ToolUseLoop 执行不上的场景回退到 SkillExecutorRegistry；Skill 也可作为 Tool 暴露给 LLM（skill 描述进系统提示词）。
      - 完成判据：外部 Skill 能以 Tool 形式被 LLM 调用。
      - 当前状态：未开始。★ [UNVALIDATED] 无端到端验证。

  - [ ] **T5. 高级能力（按需逐步推进）**
    - 当前状态：未开始。以下均为增强项，不阻塞 T2 闭环交付。
    - [ ] T5.1 开启 ReflectionEngine 联动
      - 内容：ToolUseLoop 执行失败（工具抛异常/结果异常）时，接入 `ReflectionEngine` 诊断 + `Replanner` 重规划，有上限重试。
      - 完成判据：构造"工具返回空结果"场景，能自动换工具/换参数重试。
      - 当前状态：未开始。★ [UNVALIDATED] 无端到端验证。
    - [ ] T5.2 会话记忆（跨请求上下文）
      - 内容：把历史 ToolUseLoop 对话摘要存入会话（复用 SessionService / ExperienceMemory），新请求可延续上下文。
      - 完成判据：连续两个请求（先查仓库、再基于结果建文档）第二条能引用第一条结果。
      - 当前状态：未开始。
    - [ ] T5.3 复杂任务多步编排
      - 内容：对"先搜文档→提取内容→生成摘要→保存"类多步任务，验证 ToolUseLoop 能自主串联（而非预设子任务）。
      - 完成判据：端到端真实执行成功且产物正确。
      - 当前状态：未开始。★ [UNVALIDATED] 无端到端验证。
    - [ ] T5.4 工具调用序列固化 Skill
      - 内容：把用户认可的重复工具序列（如"新建仓库+上传文档+分享"）固化为 Skill（复用 SkillCrystallizer / ExperienceMemory）。
      - 完成判据：重复请求可命中已固化 Skill 一键执行。
      - 当前状态：未开始。

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
    - [ ] T6.3 默认开启 ToolUseLoop（灰度翻正）
      - 内容：`agent.tool-loop.enabled=true`；保留开关随时回退旧路径。
      - 完成判据：真实请求默认走 ToolUseLoop；监控无异常回退。
      - 当前状态：未开始。
    - [ ] T6.4 长尾回归
      - 内容：旧路径曾支持的意图/命令（list-repos、create-repos 等复合命令）在新路径下仍可用。
      - 完成判据：覆盖旧路径全部命令场景，无能力退化。
      - 当前状态：未开始。

---

## 验收标准（两级，已定）

1. **功能级**：ToolUseLoop 能自主完成"搜索→RAG→回答""列出仓库→查看文档→生成摘要"等多步任务，产物与旧路径/人工一致。
2. **工程质量级**：新旧路径并存可灰度回退；工具调用全审计、写操作全确认；无死循环、无上下文膨胀失控；性能不劣于旧路径。

## 明确不做（边界）

- 不做通用编程 Agent（本 Agent 限定 DocSys 文档管理域）。
- 不改 `DocSysClient` 底层（只在其上做 Tool 封装）。
- 不重写 `LLMService`（ToolUseLoop 只新增调用方）。
- 不改旧 `decomposeTask` 路由代码（T6.3 翻正前旧路径保持原样）。
