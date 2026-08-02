# GOAI 世界人工智能开源大赛 · 初赛提交 · 方案 PPT 大纲

> 赛道：新智基座（Agent Infra） | 建议 14–16 页 | 标注 ★ 为赛道点名必须覆盖的要素
> 项目开源：https://gitee.com/RainyGao/DocSys

---

## 第 1 页 · 封面

- **标题**：MxsDoc AI —— 基于 AgentTeams 的企业文档多 Agent 协同基座
- **副标题**：让多 Agent 从 Demo 走向 Production，构建在真实开源文档系统之上
- 赛道：新智基座（Agent Infra） | 个人参赛
- 开源：gitee.com/RainyGao/DocSys（GPL 2.0）
- 技术栈：Java 8 + Spring MVC + MyBatis + OkHttp + Micrometer + Bucket4j

---

## 第 2 页 · 问题与场景 ★场景价值

**行业痛点**：
- 单 Agent 实验室能跑通 Demo，一进真实企业系统就"水土不服"——**缺权限隔离、缺操作审计、缺稳定接入、缺可观测**。
- 企业文档管理场景中，用户需要"查文档 / 建仓库 / 上传文件 / 全文检索 / 知识问答"，但操作必须**严格按登录用户权限执行**，写操作要可确认、可审计、可回溯。

**具体场景**（已在 MxsDoc 中运行）：
- 用户输入自然语言 → Agent 自动识别意图 → 调用对应工具 → 返回结果
- 例如："列出我的仓库" → `list_repos` → 返回仓库列表
- 例如："在 DocSys 仓库中搜索 Kafka 相关文档并读取第一篇" → `search_docs` → `get_doc` → 总结内容（LLM 自主 3 步串联，实测通过）

**一句话价值**：真正的挑战不是让一个 Agent 更聪明，而是让一组 Agent 在真实企业系统里**可信、可控、可观测**地协同工作。

---

## 第 3 页 · 核心价值主张

三个"真"：
- **真实系统**：构建在 MxsDoc 开源文档系统之上，非模拟场景
- **真实权限**：Agent 与 MxsDoc 共享登录会话，以当前用户真实权限执行
- **真实审计**：写操作全程留痕，SHA-256 脱敏，SSE 二次确认

三个"可"：
- **可信**：权限隔离 + 写操作审批 + 限流保护
- **可控**：五态任务状态机 + 超时恢复 + 失败重试上限
- **可观测**：Micrometer 指标 + 健康探针 + MDC 日志 + Step 审计

**复用价值**：任何企业系统均可按此模式（可插拔 Skill 执行器 + SKILL.md 标准 + 审计/限流横切）接入多 Agent 能力。

---

## 第 4 页 · AgentTeams 整体架构 ★Agent 分工

**一张架构图**（见附录 ASCII 版，用绘图工具美化）：

5 个职能 Agent 协同工作，以 AgentTeams 为协同设计基点：

```
用户自然语言请求
        │
        ▼
┌─────────────────────────────────────────────────┐
│  编排 Agent (MainAgent)                          │
│  意图识别 → 任务拆解 → 分派 → 聚合 → 生命周期收口  │
└──┬────────┬────────┬────────┬──────────────────┘
   │        │        │        │
   ▼        ▼        ▼        ▼
┌──────┐ ┌──────┐ ┌──────┐ ┌──────────────┐
│推荐   │ │进化   │ │反思   │ │ 执行 Agent    │
│Agent  │ │Agent  │ │Agent  │ │ (SubAgent池)  │
│协同过滤│ │技能结晶│ │失败重划│ │ 工具调用执行   │
└──────┘ └──────┘ └──────┘ └──────┬───────┘
                                  │
                    ┌─────────────▼──────────────┐
                    │  ToolUseLoop 推理引擎 (REPL) │
                    │  LLM自主决策→调工具→观察→推理 │
                    └─────────────┬──────────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              ▼                   ▼                   ▼
        ┌──────────┐      ┌──────────┐       ┌──────────┐
        │ 内置Skill │      │ 外部Skill │       │ LLM 对话  │
        │ Java直接调│      │Python/CLI │       │ 多模型    │
        └─────┬────┘      └─────┬────┘       └─────┬────┘
              └───────────────────┼───────────────────┘
                                  ▼
              ┌─────────────────────────────────────┐
              │  MxsDoc 企业文档系统底座               │
              │  用户·仓库·文档·搜索·RAG·备份 40+ 接口   │
              │  共享会话 · 真实权限 · 写操作审计        │
              └─────────────────────────────────────┘

横切能力：安全审批(SSE二次确认) · 审计脱敏(SHA-256) · 限流(Bucket4j)
         · 可观测(Micrometer/健康探针/MDC/Step审计) · 任务持久化(五态状态机)
```

---

## 第 5 页 · 5 个 Agent 职能详表 ★Agent 分工

| # | Agent 角色 | 组件 | 核心职责 | 运转状态 |
|---|-----------|------|---------|---------|
| 1 | **编排 Agent** | `MainAgent` | 意图识别、任务拆解（`decomposeTask`）、分派（`executeSubTasks`）、结果聚合 | 每请求真实运行 |
| 2 | **执行 Agent** | `SubAgent`（按任务类型池化） | 将子任务映射到 25 个工具的调用或 LLM 对话，结果回传 | 每子任务真实运行 |
| 3 | **反思 Agent** | `ReflectionEngine` + `Replanner` | 执行失败 → LLM 诊断 → 重规划 → 有上限重跑，异常自愈 | 实装完整，开关治理（默认关闭保零回归） |
| 4 | **进化 Agent** | `SkillCrystallizer` + `ExperienceMemory` + `SelfDiagnostics` | 记忆经验、自诊断、重复成功任务"结晶"为可复用 Skill | 记忆/诊断每请求运行；结晶按阈值触发 |
| 5 | **推荐 Agent** | `CollaborativeFilteringService` + `BehaviorTracking` | 行为打标签、按用户相似度协同推荐最优策略 | 接线完整，依赖数据积累 |

**AgentTeams 协同机制**：
- 编排 Agent 是协同中枢，统一收口所有 Agent 的生命周期
- 推荐 Agent 在拆解前注入协同信号（相似用户的成功经验）
- 进化 Agent 异步监听任务结果，达标触发结晶
- 反思 Agent 在失败路径上接管，与编排 Agent 协作完成自愈
- 执行 Agent 池化管理，按任务类型复用实例

---

## 第 6 页 · 任务拆解与端到端闭环 ★任务拆解 ★上下文传递

**双路径并存，灰度切换**：

**路径一：ToolUseLoop 推理引擎（主路径，默认开启）**
- LLM 作为推理引擎，收到 25 个工具的 JSON Schema 定义
- 自主决定：调哪个工具 → 观察结果 → 再推理 → 最多 10 轮（防死循环）
- 支持流式输出：reasoning（推理过程）→ tool_call（工具调用）→ tool_result（工具结果）→ text（最终回答）逐步推送到前端
- 实测验证：`list_repos → search_docs → get_doc → 总结` 4 轮自主串联，结果正确

**路径二：规则路由（旧路径，保留兜底）**
- 复合命令 → 规则直映射
- 自然语言 → LLM 意图解析（confidence≥0.6）→ Skill trigger 打分匹配 → 中文正则兜底 → chat 兜底

**上下文传递机制** ★上下文传递：
- `AgentContext`：会话上下文（sessionId / userId / requestId）
- `DocSysClient.copyWithSession()`：per-session 客户端，携带 JSESSIONID，保证多用户隔离
- MDC 结构化日志：`requestId` / `sessionId` / `userId` 串联全链路
- 会话历史注入：ToolUseLoop 加载最近 20 条对话历史，实现上下文延续

---

## 第 7 页 · Skill 工程体系 ★Skill（赛道核心，25% 权重）

**可插拔架构**：
```
SkillExecutor 接口
    ├── @Order(100) DocSysSkillExecutor    —— 内置 Skill，Java handler → DocSysClient
    ├── @Order(200) ExternalSkillExecutor   —— 外部 Skill，从磁盘 skills/<id>/ 加载
    └── Spring 自动装配 SkillExecutorRegistry —— 新增执行器无需改调度器
```

**外部 Skill 三段式执行**（fallback 链）：
1. **Python 脚本**：`__main__.py`，ProcessBuilder + stdin/stdout JSON，超时可配
2. **CLI Command**：解析 skill.md 的 `## CLI Command` 块，占位符插值
3. **LLM Guidance**：读 agent.md + skill.md 构造 system prompt 调 LLM

**标准对齐**：SKILL.md / agent.md（agentskills.io 风格）双向解析与导出

**匹配与质量评估**：
- Trigger 打分：精确 1.0 → startsWith 0.8 → contains 0.6
- 可见性控制：PUBLIC / TENANT / PRIVATE 三级
- 版本管理：Skill 结晶自动生成版本号
- 安全边界：`toRealPath()` + `startsWith(skillsDir)` 防路径穿越

**复用价值**：第三方只需在 `skills/<id>/` 目录放入 SKILL.md + 执行脚本，即可零代码扩展 Agent 能力，无需修改 Java 源码。

---

## 第 8 页 · 工具集成与 MCP ★工具集成

**25 个企业级工具**（JSON Schema 定义，LLM 可见）：

| 分组 | 数量 | 说明 |
|------|------|------|
| 只读工具 | 13 | `list_repos` / `get_doc` / `search_docs` / `get_doc_history` / `get_login_user` / `rag_chat` / `web_search` / `memory_get` / `memory_list` … |
| 写工具 | 12 | `create_repos` / `delete_repos` / `create_doc` / `delete_doc` / `rename_doc` / `move_doc` / `upload_doc` / `lock_doc` / `memory_set` … |

**工具接入协议**（等价 MCP 集成契约）：
- 每个工具 = `ToolDefinition`（name / description / parameters JSON Schema / executor / isWrite / needsConfirm）
- `DocSysClient` 用 OkHttp 稳定封装 MxsDoc 后端 40+ 个 `.do` 接口
- 写工具统一 `needsConfirm=true`，走 SSE 审批门（`WriteConfirmGate`）
- 工具执行结果自动截断（4000 字符），防上下文溢出
- 工具调用全程 Step 审计（`[ToolUseLoop][STEP]` 结构化日志）

**RAG 能力落地**（4 项中 ≥2 项）：
1. ✅ **检索增强生成**：`rag_chat` 工具，走 DocSys RAG 管线（文档向量化 + 语义检索）
2. ✅ **全文搜索**：`search_docs` 工具，走 DocSys 全文检索引擎
3. ✅ **会话历史注入**：最近 20 条对话作为上下文增强
4. ✅ **用户记忆持久化**：`memory_set/get/list` 工具，跨会话记忆复用

---

## 第 9 页 · 结果验证与异常处理 ★结果验证 ★异常分支

**结果验证机制** ★结果验证：
- 编排层校验：成功子任务数 ≥ 期望数 → 标记 COMPLETED
- SelfDiagnostics：滑动窗口（200 条）计算成功率 + P50/P95/P99 延迟
- 错误率 > 30% 或 P95 > 5s → 告警（30 分钟去重）
- ToolUseLoop：LLM 自主验证工具返回结果，结果不符合预期 → 换策略重试
- 防死循环：MAX_TURNS=10 + MAX_IDENTICAL_CALLS=3 + MAX_MALFORMED=3

**异常分支全覆盖** ★异常分支：
```
任务执行
  ├── 成功 → COMPLETED → 聚合结果
  ├── 失败 → retryCount < maxRetries(3) → RETRY_PENDING 重新入队
  │      └── 重试成功 → COMPLETED
  │      └── 重试耗尽 → FAILED
  ├── 超时 → timeoutAt(默认5min)超 → FAILED
  ├── 工具调用畸形 → ToolCallParser 容错（XML/JSON 多格式）→ 最多3次重解析
  │      └── 仍失败 → 注入"请用正确格式"提示 → LLM 重试
  ├── 反思路径（开启时）→ ReflectionEngine LLM诊断 → Replanner 重规划 → 重新分派
  └── 模型输出漂移 → 多格式兼容（`<tool_call>` / `<tool_calls>` / Anthropic XML）
```

**可靠性保障**：MySQL/SQLite 持久化任务队列，五态状态机（PENDING / RUNNING / COMPLETED / FAILED / RETRY_PENDING），启动可恢复 stale 任务。

---

## 第 10 页 · 安全边界与风险控制 ★安全边界 ★风险控制

**写操作审批闭环**：
```
用户请求写操作
  → ToolUseLoop 识别 needsConfirm 工具
  → WriteConfirmGate 拦截
  → AuditLogService 生成 PENDING 审计记录 + confirmToken
  → SSE 推送确认卡片（中文风险提示，含操作类型/参数摘要）
  → 用户 approve → 执行写操作 → 审计记录 → APPROVED → COMPLETED
  → 用户 reject → 审计记录 → REJECTED，操作不执行
  → 120s 超时 → 自动 REJECTED
```

**审计与脱敏**：
- 仅审计写操作（降低存储与性能开销）
- 敏感字段（password / token / cookie / content …）SHA-256 哈希或 `[REDACTED]`
- 审计状态机：PENDING → APPROVED/REJECTED → COMPLETED/FAILED
- Step 审计：每步工具调用记录 `[ToolUseLoop][STEP]` 结构化日志（sessionId / requestId / turn / tool / args / success / durationMs）

**限流保护**：
- Bucket4j 令牌桶：per-IP / per-API-key 独立限流
- 每 IP 并发 SSE 连接限制
- 超限返回 429 + `Retry-After` 头

**鉴权双模**：
- 同源：共享 MxsDoc HttpSession，免 API Key
- 对外：`X-API-Key` / `Authorization: Bearer` + 同源 session 双模

**风险控制总结**：写操作不可绕过审批 → 不可跳过审计 → 不可突破限流 → 不可越权访问。

---

## 第 11 页 · 可观测性 ★（赛道推荐，从 Demo 走向 Production）

| 能力 | 实现 | 状态 |
|------|------|------|
| **指标** | Micrometer：`agent.tool.call` / `agent.tool.loop` / `agent.execution` Timer + `agent.active.sessions` Gauge | ✅ 已打点 |
| **健康探针** | `GET /api/agent/health`：DocSys / LLM / Session 三探针聚合 | ✅ 已实现 |
| **自诊断** | `SelfDiagnostics`：滑动窗口 P50/P95/P99 + 错误率阈值告警（去重） | ✅ 每请求运行 |
| **结构化日志** | MDC：`requestId` / `sessionId` / `userId` 串联全链路 | ✅ 已实现 |
| **Step 审计** | `[ToolUseLoop][STEP]` 每步工具调用可回溯，进 `docsys.log` | ✅ 已实现 |
| **执行证据链** | 任务队列 + 审计表 + Step 日志 → 完整可复现证据 | ✅ 可追溯 |
| **Prometheus 端点** | 当前用内存 `SimpleMeterRegistry`，暴露抓取端点为复赛计划 | 📋 计划中 |
| **TraceId 贯通** | 字段预留，MDC 贯通为复赛计划 | 📋 计划中 |
| **可视化 Trace 页面** | 多 Agent 协同过程可视化，复赛计划 | 📋 计划中 |

---

## 第 12 页 · 自进化能力（差异化亮点）

**经验记忆**（真实运转）：
- 多层记忆记录每次执行结果
- 识别历史意图，复用成功经验
- `memory_set/get/list` 工具支持用户级持久记忆

**技能结晶**（从经验到可复用 Skill）：
```
某任务类型重复成功 N 次（阈值=3）
  → SkillCrystallizer 检查无内置 Skill 覆盖
  → 自动生成 SKILL.md（含 name / description / trigger / parameters / version）
  → EnhancedSkillManager 解析注册为新 Skill
  → 下次同类请求优先匹配，跳过 LLM 推理，直接执行
```

**自诊断**（持续健康监控）：
- 滑动窗口实时统计成功率与延迟
- 异常自动告警，辅助运维决策

**定位**：不是一次性的脚本或演示，而是具备"从成功经验中提取 → 标准化 → 注册 → 复用"完整闭环的自进化机制。结晶阈值治理、低频触发，避免过度自动化。

---

## 第 13 页 · 可行性与落地计划 ★

**已完成（初赛阶段）**：
- ✅ Agent 合并进 MxsDoc 同一 WAR 应用，共享会话/数据源/LLM 配置
- ✅ ToolUseLoop 流式推理引擎为主路径，LLM 自主多轮工具调用
- ✅ 25 个工具全部定义、注册、端到端验证通过
- ✅ 写操作 SSE 二次确认 + SHA-256 审计脱敏 + Bucket4j 限流
- ✅ 可插拔 Skill 执行器 + SKILL.md 标准 + 外部 Skill 三段式执行
- ✅ Micrometer 指标 + 健康探针 + MDC 日志 + Step 审计
- ✅ MySQL/SQLite 双方言 + 任务持久化五态状态机
- ✅ Memory 工具 / Web Search 工具 / 管理后台提示词配置
- ✅ 249+ 单元测试全部通过

**复赛计划（8.25–9.3）**：
- 📋 多 Agent 协同可视化 Trace 页面
- 📋 打通 traceId 全链路（MDC 贯通）
- 📋 暴露 Prometheus 指标抓取端点
- 📋 补充可复现部署脚本与样例证据包
- 📋 补充端到端集成测试（覆盖异常分支）
- 📋 整理 Skill 开发者文档与贡献指南

**落地路径**：
1. 先在文档系统场景深度打磨（当前阶段）
2. 抽象可插拔 Skill + 审计/可观测能力为通用 Agent 基座
3. 供其它企业系统（运维/客服/风控）按同一模式接入

---

## 第 14 页 · 开放与开源 ★开源计划（5% 权重）

**开源现状**：
- 项目已在 Gitee 开源：https://gitee.com/RainyGao/DocSys
- 许可证：GPL 2.0
- Agent 模块完整源码在 `src/com/DocSystem/agent/` 下

**开放计划**：
- 整理 Agent 模块架构文档、部署说明、SKILL.md 样例
- 补充 README 中的运行证据（截图/日志样例）
- 编写 Skill 开发者指南：如何编写一个外部 Skill
- 提供 Docker 一键部署脚本（降低试用门槛）

**复用接口契约**：
| 扩展点 | 契约 | 说明 |
|--------|------|------|
| SkillExecutor 接口 | `execute(SkillExecutionRequest) → SkillResult` | 新增 Skill 类型只需实现此接口 + `@Order` 优先级 |
| ToolDefinition | name / description / parameters(JSON Schema) / executor / isWrite / needsConfirm | 新增工具只需构造此对象 + 注册进 ToolRegistry |
| SKILL.md 标准 | YAML frontmatter + Markdown 正文 | 第三方可按此格式贡献 Skill，无需改 Java 代码 |
| WriteConfirmGate 接口 | `needsConfirm(ToolDefinition, args) → boolean` + `onApproved/onRejected` | 自定义审批逻辑 |
| StepAuditSink 接口 | `onStep(StepAuditEvent)` | 自定义审计输出（默认写 docsys.log） |

**合作愿景**：任何企业系统（运维、客服、风控、软件开发协同）均可按此模式——接入自身业务接口为 Tool → 定义领域 Skill → 启用审计/限流/可观测横切能力 → 快速获得多 Agent 协同能力。

---

## 第 15 页 · 与赛道要求的逐项对标

| 赛道要求 | MxsDoc AI 对应实现 | 状态 |
|----------|-------------------|------|
| ≥3 个不同职能 Agent | 5 个：编排 / 执行 / 反思 / 进化 / 推荐 | ✅ |
| Skill 可复用 | SkillExecutor 可插拔 + SKILL.md 标准 + 外部三段式执行 | ✅ |
| AgentTeams 协同基点 | 编排 Agent 统一收口 + 4 个协作 Agent + ToolUseLoop 引擎 | ✅ |
| MCP 等价集成契约 | ToolDefinition JSON Schema + DocSysClient 40+ 接口 | ✅ |
| RAG（4 项 ≥2） | rag_chat + search_docs + 会话历史 + 用户记忆 | ✅ 4/4 |
| 可观测 | Micrometer + 健康探针 + MDC + Step 审计 + 自诊断 | ✅ |
| 端到端闭环 | 拆解→传递→调用→验证→审计 完整链路 | ✅ |
| 安全可审计 | SSE 二次确认 + SHA-256 脱敏 + 限流 + 状态机 | ✅ |

---

## 第 16 页 · 结语

**一句话收尾**：
**MxsDoc AI 用一个真实运行的开源文档系统，证明多 Agent 能在企业生产环境里可信、可控、可观测地协同工作——不是 PPT 造车，是 249+ 测试护航、端到端验证通过的生产级基座。**

**口号**：Open. Share. Build. —— 从文档系统出发，通向通用多 Agent 协同基座。

---

## 附录：架构图 ASCII 源（供绘图软件美化）

```
                          用户（自然语言请求）
                                  │
                    ┌─────────────▼──────────────┐
                    │      编排 Agent  MainAgent   │
                    │  意图识别→任务拆解→分派→聚合  │
                    └──┬───────┬───────┬──────┬───┘
        推荐Agent ──────┘       │       │      └──────── 进化Agent
  CollaborativeFiltering       │       │        SkillCrystallizer
  （相似用户/协同推荐）          │       │        ExperienceMemory
                               │       │        SelfDiagnostics
                    ┌──────────▼───┐   └──► 反思Agent（开关治理）
                    │ 执行Agent     │        ReflectionEngine
                    │ SubAgent 池  │        Replanner
                    └──────┬───────┘
                           │  ToolUseLoop 推理引擎 (REPL)
              ┌────────────┼─────────────┐
        内置 Skill      外部 Skill      LLM 对话
     DocSysSkillExecutor  Python/CLI    LLMService
              │                          （多模型按需取）
              ▼
   ┌───────────────────────────────────────────┐
   │  企业系统底座  MxsDoc / DocSystem           │
   │  用户·仓库·文档·搜索·RAG·备份 40+ 接口       │
   │  共享登录会话 · 真实权限 · 写操作审计         │
   └───────────────────────────────────────────┘

横切能力：安全审批(SSE二次确认) · 审计脱敏(SHA-256) · 限流(Bucket4j)
          · 可观测(Micrometer/健康探针/MDC/Step审计) · 任务持久化(五态状态机)
```

---

## 附录 B：代码包清单（可选加分项，复赛前完善）

若提交代码包，需包含：
- **运行入口**：MxsDoc WAR 部署说明（Tomcat 8 + JDK 8），Agent 页面入口 `/DocSystem/web/agent/index.html`
- **依赖说明**：无 Maven（企业内网限制），jar 置于 `WEB-INF/lib`；数据库支持 MySQL / SQLite
- **配置文件**：`jdbc.properties`、LLM 配置（复用 DocSystem `systemLLMConfig`）
- **样例输入输出**：
  - `Hello` → AI 流式回复（含 reasoning 灰色推理过程可见）
  - `列出我的仓库` → `list_repos` → 返回仓库列表
  - `搜索 Kafka 相关文档并总结第一篇` → `search_docs` → `get_doc` → LLM 总结（3 步自主串联）
- **运行证据**：SSE 流日志、审计表记录、`/api/agent/health` 输出、任务队列状态、Step 审计日志
- **测试**：249+ 单元测试（`TestToolRegistry` / `TestToolCallParser` / `TestToolUseLoop` / `TestToolUseLoopStreaming` / `TestWriteTools` / `TestUserMemoryTools` / `TestWebSearchTool` / `TestStepAudit` / `TestAgentConfig`）

---

## 附录 C：材料撰写诚实声明

**可放心主张**（每请求真实运转 / 完整实装）：
- 5 个 Agent 协同架构（编排 / 执行 / 反思 / 进化 / 推荐）
- ToolUseLoop 流式推理引擎（LLM 自主多轮工具调用）
- 25 个工具（13 只读 + 12 写）JSON Schema 定义 + 端到端验证
- Skill 可插拔工程化（接口 + 优先级 + Spring 自动装配）
- 外部 Skill 三段式执行（Python / CLI / LLM）
- SKILL.md 标准双向解析与导出
- 写操作 SSE 二次确认 + SHA-256 审计脱敏
- Bucket4j 令牌桶限流 + 429 响应
- Micrometer 指标 + 健康探针 + MDC 结构化日志 + Step 审计
- MySQL/SQLite 任务持久化 + 五态状态机 + 超时恢复
- 249+ 单元测试全部通过

**需如实标注**（默认关闭 / 低频 / 预留）：
- 反思-重规划循环：实装完整但默认关闭（`enabled=false`），表述为"能力具备、开关治理"
- 技能结晶：受阈值（3 次）+ 去重约束，低频触发，勿说"高频自动进化"
- 任务执行：当前顺序执行为主，勿夸大"多子任务并行"
- 可观测：当前用内存 MeterRegistry，无 Prometheus 抓取端点；traceId 预留未贯通 → 列为复赛计划
- 协同推荐：含数处占位实现，依赖数据积累
- 外部 Skill：无 HTTP 上传端点，依赖运维投放目录
