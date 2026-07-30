# MxsDoc AI Agent —— 文档系统的多 Agent 生产级基座

> MxsDoc（DocSystem）内置的 AI 智能体模块。本文档面向开发者与评审，说明其架构、职能划分、Skill 工程化、可观测与安全审计能力，以及部署与运行方式。
>
> 所属项目：[MxsDoc](https://gitee.com/RainyGao/DocSys)（GPL 2.0） · 包路径：`com.DocSystem.agent`

---

## 1. 它是什么

MxsDoc AI Agent 是**直接构建在真实开源文档管理系统 MxsDoc 之上**的多 Agent 基座。与"独立部署的演示 Demo"不同，它与 MxsDoc 合并为**同一个 Web 应用**：

- **共享登录会话**：Agent 复用 MxsDoc 的 `HttpSession`，以**当前登录用户的真实权限**操作仓库与文档，无需二次登录。
- **共享数据源**：Agent 的 `agent_*` 表与 MxsDoc 同库（支持 MySQL / SQLite 双方言），由管理后台统一初始化与重置。
- **共享 LLM 配置**：对话时按需读取 MxsDoc 的多模型配置（`systemLLMConfig`），与 MxsDoc AIChat 同一套模型来源。

目标：解决"单 Agent 实验室能跑、进真实企业系统就水土不服"——权限、审计、稳定接入、可观测缺失——的产业痛点。

---

## 2. 架构总览

```
                          用户（自然语言请求）
                                  │
                    ┌─────────────▼──────────────┐
                    │      编排 Agent  MainAgent   │
                    │  意图识别→任务拆解→分派→聚合  │
                    └──┬───────┬───────┬──────┬───┘
        推荐官 ────────┘       │       │      └──────── 进化官
  CollaborativeFiltering       │       │        SkillCrystallizer
  （相似用户/协同推荐）          │       │        ExperienceMemory
                               │       │        SelfDiagnostics
                    ┌──────────▼───┐   └──► 反思官（开关治理，默认关闭）
                    │ 执行 Agent    │        ReflectionEngine
                    │  SubAgent 池  │        Replanner
                    └──────┬───────┘
                           │  统一 Skill 执行器（可插拔 @Order）
              ┌────────────┼─────────────┐
        内置 Skill      外部 Skill      LLM 对话
     DocSysSkillExecutor  Python/CLI    LLMService（多模型按需取）
              │
              ▼
   ┌───────────────────────────────────────────┐
   │  企业系统底座  MxsDoc / DocSystem            │
   │  用户·仓库·文档·搜索·RAG·备份 40+ 接口        │
   │  共享登录会话 · 真实权限 · 写操作审计          │
   └───────────────────────────────────────────┘

横切能力：安全审批(SSE二次确认) · 审计脱敏(SHA-256) · 限流(Bucket4j)
          · 可观测(Micrometer/健康探针/MDC) · 任务持久化(MySQL五态状态机)
```

**技术栈**：Java 8 + Spring MVC（WAR）+ MyBatis + OkHttp + Micrometer + Bucket4j。无 Maven（适配企业内网限制），依赖 jar 置于 `WEB-INF/lib`。

---

## 3. 多 Agent 职能划分

架构为「双层 Agent + 三大自治子系统」。下表如实标注每个组件的运转状态。

| 角色 | 组件（`com.DocSystem.agent.*`） | 职责 | 运转状态 |
|---|---|---|---|
| **编排 Agent** | `orchestrator.MainAgent` | 意图识别、任务拆解（`decomposeTask`）、分派（`executeSubTasks`）、结果聚合、生命周期收口 | 每请求真实运行 |
| **执行 Agent** | `orchestrator.SubAgent`（按任务类型池化） | 将子任务映射到企业接口（`DocSysClient`）或 LLM 的实际调用 | 每子任务真实运行 |
| **反思官** | `orchestrator.reflection.ReflectionEngine` + `Replanner` + `OrchestrationLoop` | 执行失败→LLM 诊断是否重试→LLM 重规划→有上限重跑 | **实装完整，默认关闭**（`ReflectionConfig.enabled=false`），以开关治理 |
| **进化官** | `evolution.SkillCrystallizer` + `ExperienceMemory` + `SelfDiagnostics` + `EvolutionTrigger` | 记忆经验、自我诊断、把重复成功的任务"结晶"为可复用 Skill | 记忆/诊断每请求运行；**Skill 结晶按阈值低频触发** |
| **推荐官** | `learning.CollaborativeFilteringService` + `BehaviorTrackingService` | 行为打标签、按用户相似度做协同推荐 | 接线完整，依赖数据积累；含少量占位实现 |

### 一次任务闭环（默认路径）

```
用户请求 MainAgent.process()
  → [推荐官] 取协同推荐（可能为空）
  → [记忆]   识别历史意图
  → [编排]   decomposeTask 拆解为 SubTask（当前多为 1 个）
  → [记忆/推荐] 记录拆解 + 行为打标签
  → [编排]   executeSubTasks 顺序分派
        → 从 subAgentPool 取执行 Agent
        → [执行] SubAgent.execute 调 DocSysClient / LLM 实际干活
  → [编排]   aggregateResults 聚合
  → [记忆/诊断] 记录执行结果 + P95 延迟
  → [进化官] onTaskSuccess 异步 → 累计成功达阈值且无内置 Skill → 结晶新 Skill
  → [推荐官] 存经验；失败时把推荐附加到响应
```

> 诚实边界：反思-重规划闭环仅在开启时接入；当前任务执行以顺序为主，多子任务并行为架构预留能力。

---

## 4. Skill 工程化

**可插拔架构**：`SkillExecutor` 接口 + `@Order` 优先级 + Spring 自动装配（`SkillExecutorRegistry`）。新增执行器无需改动调度器，是真正的工程化扩展点。

| 类型 | 执行器（`@Order`） | 加载与执行 |
|---|---|---|
| 内置 Skill | `DocSysSkillExecutor`（100） | Java handler → `DocSysClient` 调 MxsDoc API；权威清单 `BUILT_IN_SKILL_IDS`（约 60 项） |
| 外部 Skill | `ExternalSkillExecutor`（200） | 从磁盘 `skills/<id>/` 加载，三段式 fallback：**① Python 脚本**（`__main__.py`，ProcessBuilder + stdin/stdout JSON，超时可配）→ **② CLI Command**（解析 skill.md 的 `## CLI Command` 块，占位符插值）→ **③ LLM Guidance**（读 agent.md + skill.md 构造 system prompt 调 LLM） |

- **匹配**：`EnhancedSkill.getMatchScore()` trigger 打分（精确 1.0 → startsWith 0.8 → contains 0.6）。NLU 流程为 **LLM 意图解析优先**（confidence≥0.6）→ Skill trigger 打分（≥0.6）→ 中文正则兜底 → chat 兜底。
- **标准对齐**：SKILL.md / agent.md（agentskills.io 风格）双向解析与导出（`toSkillMarkdown()`）。
- **安全边界**：`toRealPath()` + `startsWith(skillsDir)` 防路径穿越；`SkillMetadata.visibility`（PUBLIC/TENANT/PRIVATE）可见性访问控制。

> 诚实边界：内置 Skill 清单目前分散在三处（`EnhancedSkillManager` 9 项 / `SkillManager` 27 项 / Executor 白名单约 60 项），未统一来源；`EnhancedSkillManager` 虽标 `@Deprecated` 但仍是 NLU 匹配主路径；外部 Skill 无 HTTP 上传端点，依赖运维投放目录。

---

## 5. 工具 / 企业系统接入

`client.DocSysClient` 用 OkHttp 稳定封装 MxsDoc 后端 **40+ 个 `.do` 接口**：

- **User**：login / logout / getLoginUser / register
- **Repos**：getReposList / getRepos / addRepos / deleteRepos / updateReposInfo / getAiModelList / backupRepos / AIChat …
- **Doc**：getDocList / addDoc / deleteDoc / renameDoc / moveDoc / copyDoc / getDoc / downloadDoc / searchDoc / uploadDoc / lockDoc / unlockDoc / createDocShare …
- **Query**：addDocSysRagMessage（RAG）
- **Manage**：getDocSysConfig / getBannerConfig / docSysInit …

要点：连接/读/写超时 30/60/60s；`copyWithSession()` 支持 per-request 多用户会话隔离；上传支持 multipart / 断点续传 / 文件夹批量。

---

## 6. 可观测性

- **指标**（Micrometer）：`agent.execution`（Timer, tag: intent/success）、`llm.call`（Timer, tag: model/success）、`agent.active.sessions`（Gauge）。已在请求路径打点。
- **健康探针**（自研，非 Actuator）：`DocSysHealthIndicator` / `LLMHealthIndicator` / `SessionHealthIndicator`，聚合于 `GET /api/agent/health`。
- **自诊断**：`SelfDiagnostics` 滑动窗口（200）计算 P50/P95/P99、成功率，错误率>30% 或 P95>5s 告警（30 分钟去重）。
- **结构化日志**：MDC 关联 `requestId` / `sessionId` / `userId`。

> 诚实边界：当前 `meterRegistry` 为内存 `SimpleMeterRegistry`，**未暴露 Prometheus 抓取端点**；`traceId` 审计字段已预留但尚未 `MDC.put` 贯通（列为后续计划）。

---

## 7. 安全审计与审批

- **写操作审计**（`controller.AuditLogService` + `entity.AuditLogEntity`）：仅审计写操作；敏感字段（password/token/cookie/content…）**SHA-256 哈希**或 `[REDACTED]`；状态机 PENDING→APPROVED/REJECTED→COMPLETED/FAILED。
- **写操作二次确认（SSE confirm）**：命中写操作 → 生成 `confirmToken` + PENDING 审计 → SSE 推 `{"type":"confirm",...}` 中文风险提示 → 用户 `POST /api/agent/confirm` approve/reject 后才执行（120s 超时）。
- **限流**（`monitoring.RateLimitService`，Bucket4j）：per-IP / per-API-key 令牌桶 + 每 IP 并发 SSE 连接限制，超限返回 429 + Retry-After。
- **鉴权**（`controller.ApiAuthInterceptor`）：与 MxsDoc 共享 session（同源免 API Key）；对外 API 走 `X-API-Key` / `Authorization: Bearer`。

---

## 8. 任务持久化与可靠性

`orchestrator.TaskQueueService` + `entity.TaskEntity` + `repository.TaskRepository`（MyBatis，`agent_tasks` 表）：

- 五态状态机：PENDING / RUNNING / COMPLETED / FAILED / **RETRY_PENDING**。
- 入队设 `timeoutAt = now + timeout-minutes`（默认 5）；失败自动重试（`retryCount < maxRetries`，默认 3）→ RETRY_PENDING 重新入队 → 耗尽转 FAILED。
- 启动恢复：`getPendingOrRunningTasksForRecovery()` 扫描 stale 任务 `reEnqueue`（当前为手动触发）。

---

## 9. 部署与运行

### 前置
- JDK 8、外部 Tomcat（javax.servlet）
- 数据库：MySQL 或 SQLite（`WEB-INF/classes/jdbc.properties` 的 `db.type` 控制）
- LLM：在 MxsDoc 管理后台配置模型（Agent 对话按需读取，默认取第一个模型）

### 步骤
1. 构建 MxsDoc（Agent 已合并入同一 WAR），部署到 Tomcat。
2. 首次访问触发 `docSysInit` 建库；`agent_*` 表由 `DatabaseInitializer` 随之创建。
3. 浏览器登录 MxsDoc（Agent 复用该登录）。
4. 访问 Agent 页面：`/DocSystem/web/agent/index.html`。

### 数据库重置
管理后台"重置数据库"会一并清除并按当前 schema 重建 Agent 表（`AgentDBTabNameMap` + `AgentInitService.rebuildTables()`）。

### 样例
- 输入 `Hello` → LLM 流式返回问候。
- 输入 `list-repos` → 按**当前登录用户权限**返回其可见仓库列表。

---

## 10. 目录结构（`com.DocSystem.agent`）

```
agent/
├── controller/      AgentController(REST/SSE)、AuditLogService、ApiAuthInterceptor …
├── orchestrator/    MainAgent(编排)、SubAgent(执行)、TaskQueueService
│   └── reflection/  反思-重规划循环（ReflectionEngine/Replanner/OrchestrationLoop）
├── skill/           Skill 定义与管理；executor/ 可插拔执行器
├── llm/             LLMService（多模型按需取，OkHttp）
├── nlu/             LLMIntentParser（意图解析）
├── evolution/       自进化（SkillCrystallizer/ExperienceMemory/SelfDiagnostics …）
├── learning/        协同过滤推荐、行为追踪
├── monitoring/      指标、健康探针、限流
├── session/         会话管理
├── config/          DatabaseInitializer、AgentInitService、LlmConfigSyncService
├── client/          DocSysClient（封装 MxsDoc 40+ 接口）
└── entity/          TaskEntity、AuditLogEntity、TypeHandler …
```

---

## 11. 路线图

- [ ] 多 Agent 协同的可视化 Trace 页面（展示拆解→执行→证据链）
- [ ] 打通 `traceId` 全链路关联
- [ ] 暴露 Prometheus 指标抓取端点
- [ ] 统一内置 Skill 清单来源
- [ ] 外部 Skill 的 HTTP 上传/安装端点
- [ ] 反思-重规划的默认启用与效果评测

---

## 许可证

随 MxsDoc 采用 **GPL 2.0**。欢迎通过 `SkillExecutor` 扩展点与 SKILL.md 标准贡献 Skill。
