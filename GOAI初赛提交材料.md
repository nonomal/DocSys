# GOAI 世界人工智能开源大赛 · 新智基座赛道 · 初赛提交材料（初稿 · 已优化）

> ⚠️ **本文件为初稿，已被以下优化版本替代。请使用优化版本提交：**
> - **作品简介** → [`GOAI_作品简介.md`](GOAI_作品简介.md)
> - **方案 PPT 大纲** → [`GOAI_方案PPT大纲.md`](GOAI_方案PPT大纲.md)
>
> 优化版本根据赛道宣讲会信息重新编排：强化了 AgentTeams 协同叙事、5 个 Agent 职能定位、
> Skill 工程体系、端到端闭环证据链，补齐了 ToolUseLoop 推理引擎等最新进展，逐项对标五大评审标准。
> 本初稿保留作为历史参考。

> 赛道：新智基座（Agent Infra） | 参赛方式：个人 | 提交截止：2026-08-16
> 项目开源：https://gitee.com/RainyGao/DocSys （Gitee）
> 说明：本文档所有能力主张均对应真实代码，未实装/默认关闭/低频触发的能力已如实标注，以匹配赛道"可运行、可验证、可复现"的评审导向。

---

## 一、作品简介（必交，500 字以内）

**项目名称**：MxsDoc AI —— 文档系统的多 Agent 生产级基座（约 20 字）

**正文（约 480 字）**：

单个 Agent 在实验室能跑通 Demo，一进真实企业系统就"水土不服"——权限、审计、稳定接入、可观测缺失。MxsDoc AI 直面这一痛点：它不是又一个演示 Demo，而是构建在**真实开源文档管理系统 MxsDoc（DocSystem）** 之上、已能运行的多 Agent 基座，让 Agent 直接以登录用户的真实权限操作企业文档与仓库。

**核心方案**：采用"双层 Agent + 三大自治子系统"架构。编排 Agent（MainAgent）负责意图识别与任务拆解，执行 Agent（SubAgent）通过统一的 Skill 执行器把任务落到 40+ 个企业系统接口；反思重规划、自进化技能结晶、协同过滤推荐三个子系统横切增强。Skill 采用接口 + 优先级 + Spring 自动装配的可插拔工程化设计，内置与外部（Python/CLI/LLM 三段式）双级执行，对齐 SKILL.md 开放标准。

**创新点与差异化**：① 真实企业系统底座，Agent 与文档系统共享会话、权限、审计，天然生产级；② 完整的安全审批闭环——写操作 SHA-256 脱敏审计 + SSE 二次确认；③ 令牌桶限流、健康探针、Micrometer 指标、结构化日志构成可观测能力；④ 从成功经验"结晶"出可复用 Skill 的自进化机制。

**开放/复用价值**：项目已在 Gitee 开源；Skill 执行器可插拔、SKILL.md 标准可复用，任何企业系统均可按此模式接入多 Agent 能力。

**当前进展**：Agent 已合并进 MxsDoc 同一 Web 应用，登录鉴权、LLM 多模型对话、任务持久化、审计限流均已跑通并可演示。

---

## 二、方案 PPT 大纲与逐页要点（必交，据此制作 PPT/PDF）

> 建议 12–14 页。每页给出【标题】+【要点文字】，你套模板即可。官方点名要覆盖的要素（Agent 分工、任务拆解、上下文传递、结果验证、异常分支、安全边界、风险控制、开源计划）已分布到对应页并标注 ★。

### 第 1 页 · 封面
- 标题：**MxsDoc AI —— 文档系统的多 Agent 生产级基座**
- 副标题：让 Agent 从 Demo 走向 Production，构建在真实开源文档系统之上
- 赛道：新智基座（Agent Infra） | 个人参赛 | 开源：gitee.com/RainyGao/DocSys

### 第 2 页 · 问题与场景 ★场景
- 行业痛点：单 Agent 实验室能跑，真实生产环境"水土不服"——缺权限、缺审计、缺稳定接入、缺可观测。
- 具体场景：企业文档管理系统里，用户想用自然语言完成"列仓库 / 建仓库 / 上传文档 / 搜索 / 问答"等操作，但必须严格按登录用户权限执行，且写操作要可审计、可确认、可回溯。
- 一句话：**真正的挑战不是让一个 Agent 更聪明，而是让一组 Agent 在真实企业系统里可信、可控、可观测地干活。**

### 第 3 页 · 核心价值主张
- 不做"玩具 Demo"，做**可接入真实企业系统的多 Agent 基座**。
- 三个"真"：真实系统（MxsDoc 开源文档系统）、真实权限（共享登录会话）、真实审计（写操作全程留痕）。
- 复用价值：任何企业系统都能按同一模式接入多 Agent 能力。

### 第 4 页 · 整体架构图 ★Agent 分工
- 一张架构图（见附录 ASCII 版，用绘图工具美化）。
- 双层 Agent：**编排 Agent（MainAgent）** + **执行 Agent（SubAgent）**。
- 三大自治子系统：**反思重规划** / **自进化结晶** / **协同过滤推荐**。
- 底座：MxsDoc（DocSystem）——用户/仓库/文档/搜索/RAG/备份等 40+ 接口。

### 第 5 页 · Agent 分工与职能划分 ★Agent 分工
| 角色 | 组件 | 职责 | 状态 |
|---|---|---|---|
| 编排 Agent | MainAgent | 意图识别、任务拆解、分派、结果聚合、生命周期收口 | 每请求运行 |
| 执行 Agent | SubAgent（按任务类型池化） | 把子任务落到企业接口/LLM 的实际调用 | 每子任务运行 |
| 反思官 | ReflectionEngine + Replanner | 执行失败→LLM 诊断→重规划→有上限重跑 | 实装完整，开关治理（默认关闭以保零回归） |
| 进化官 | SkillCrystallizer + ExperienceMemory + SelfDiagnostics | 记忆经验、自诊断、把重复成功结晶为可复用 Skill | 记忆/诊断每请求运行；结晶按阈值触发 |
| 推荐官 | CollaborativeFilteringService + BehaviorTracking | 行为打标签、按相似度做协同推荐 | 接线完整，依赖数据积累 |

### 第 6 页 · 任务拆解与上下文传递 ★任务拆解 ★上下文传递
- 拆解：复合命令→规则直映射；自然语言→**LLM 意图解析优先**（confidence≥0.6）→Skill trigger 打分匹配→中文正则兜底→chat 兜底。
- 上下文传递：AgentContext（会话上下文）+ per-session DocSysClient（携带 JSESSIONID，保证多用户隔离）+ MDC 结构化日志（requestId/sessionId/userId）串联全链路。
- 诚实边界：当前多为"1 请求→1 子任务"的顺序执行；多子任务与并行为架构预留能力。

### 第 7 页 · Skill 工程化与工具集成 ★（赛道核心）
- 可插拔架构：`SkillExecutor` 接口 + `@Order` 优先级 + Spring 自动装配（`SkillExecutorRegistry`），加执行器无需改调度器。
- 两级 Skill：内置（Java handler→企业 API）+ 外部（磁盘目录，Python/CLI/LLM 三段式 fallback 执行）。
- 标准对齐：SKILL.md / agent.md（agentskills.io 风格）双向解析与导出。
- 匹配：trigger 打分（精确 1.0→contains 0.6）。
- 工具接入：DocSysClient 稳定封装 40+ 企业 `.do` 接口（用户/仓库/文档/搜索/RAG/备份/锁/分享/断点续传上传）。

### 第 8 页 · 结果验证与异常分支 ★结果验证 ★异常分支
- 结果验证：编排层校验"结果数==子任务数"判定成功；SelfDiagnostics 记录每次执行的成功率与 P50/P95/P99 延迟。
- 异常分支：任务失败→重试（retryCount<maxRetries，默认 3）→RETRY_PENDING 重新入队→耗尽转 FAILED；反思循环（开启时）对失败做 LLM 诊断与重规划。
- 可靠性：MySQL 持久化任务队列，五态状态机（PENDING/RUNNING/COMPLETED/FAILED/RETRY_PENDING），超时（默认 5min）可恢复。

### 第 9 页 · 安全边界与审批 ★安全边界 ★风险控制
- 写操作二次确认：命中写操作→生成 confirmToken + PENDING 审计→SSE 推确认卡片（中文风险提示）→用户 approve/reject 才执行。
- 审计与脱敏：仅审计写操作；敏感字段（password/token/cookie/content…）SHA-256 哈希或 [REDACTED]。
- 鉴权：与 MxsDoc 共享登录会话，Agent 以**当前登录用户真实权限**执行；对外 API 走 API Key + 同源 session 双模。
- 限流：Bucket4j 令牌桶（per-IP/key）+ 每 IP 并发 SSE 连接限制 + 429/Retry-After。

### 第 10 页 · 可观测性与执行证据 ★（赛道核心）
- 指标：Micrometer 采集 execution/llm.call Timer、active.sessions Gauge（已在请求路径打点）。
- 健康：DocSys/LLM/Session 三探针聚合于 `/api/agent/health`。
- 诊断：SelfDiagnostics 滑动窗口 P95 + 错误率阈值告警（去重）。
- 日志：MDC 结构化关联 requestId/sessionId/userId。
- 证据沉淀：任务队列、审计表、执行日志构成可复现的运行证据链。
- 诚实边界：当前用内存 MeterRegistry，Prometheus 抓取端点与 traceId 贯通为下一步（复赛计划）。

### 第 11 页 · 自进化能力（差异化亮点）
- 经验记忆：多层记忆记录每次执行、识别历史意图（真实运转）。
- 技能结晶：某任务类型重复成功达阈值且无内置 Skill → 自动生成 SKILL.md → 解析注册为新 Skill（附版本号）。
- 定位：具备"从经验沉淀可复用 Skill"的自进化机制，阈值治理、低频触发，非高频自动进化。

### 第 12 页 · 可行性与落地计划 ★
- 已完成：Agent 合并进 MxsDoc 同一应用、共享会话鉴权、LLM 多模型对话、任务持久化、审计限流、数据库双方言（MySQL/SQLite）。
- 复赛计划：多 Agent 协同的可视化 Trace 页面；打通 traceId 全链路；暴露 Prometheus 指标；补充可复现部署脚本与样例证据包。
- 落地路径：先在文档系统场景打磨，再把可插拔 Skill + 审计/可观测能力抽象为通用 Agent 基座，供其它企业系统接入。

### 第 13 页 · 开源与开放价值 ★开源计划
- 现状：已在 Gitee 开源（gitee.com/RainyGao/DocSys）。
- 计划：整理 Agent 模块的架构文档、部署说明、SKILL.md 样例；补充 README 与运行证据；持续开源迭代。
- 复用：SkillExecutor 扩展点 + SKILL.md 标准，第三方可贡献 Skill。

### 第 14 页 · 结语
- 一句话收尾：**MxsDoc AI 用一个真实运行的开源文档系统，证明多 Agent 能在企业生产环境里可信、可控、可观测地协同工作。**
- 口号呼应：Open. Share. Build.

---

## 三、附录：架构图（ASCII，供绘图美化）

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
                    ┌──────────▼───┐   └──► 反思官（开关治理）
                    │ 执行 Agent    │        ReflectionEngine
                    │  SubAgent 池  │        Replanner
                    └──────┬───────┘
                           │  统一 Skill 执行器（可插拔 @Order）
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
          · 可观测(Micrometer/健康探针/MDC) · 任务持久化(MySQL五态状态机)
```

---

## 四、可选提交：AgentTeams 代码包清单（加分项，复赛前完善）

若提交代码包，需包含：
- **运行入口**：MxsDoc WAR 部署说明（Tomcat + JDK8），Agent 页面入口 `/DocSystem/web/agent/index.html`
- **依赖说明**：无 Maven（企业内网限制），jar 置于 WEB-INF/lib；数据库支持 MySQL/SQLite
- **配置文件**：jdbc.properties、LLM 配置（复用 DocSystem systemLLMConfig）
- **样例输入输出**：`Hello` → AI 流式回复；`list-repos` → 按登录用户权限返回仓库列表
- **运行证据**：SSE 流日志、审计表记录、/api/agent/health 输出、任务队列状态

---

## 五、材料撰写原则（自我约束，避免"PPT造车"）

**可放心主张**（每请求真实运转/完整实装）：双层 Agent 架构、Skill 可插拔工程化、40+ 企业接口稳定接入、写操作审计+SSE二次确认、Bucket4j限流、Micrometer指标+健康探针、MySQL任务持久化+重试、经验记忆+自诊断。

**需如实标注**（默认关闭/低频/预留）：
- 反思-重规划循环：实装完整但默认关闭（enabled=false），表述为"能力具备、开关治理"，勿说"在跑"。
- 技能结晶：受阈值(3次)+去重约束，低频触发，勿说"高频自动进化"。
- 任务执行：当前顺序执行为主，勿夸大"多子任务并行"。
- 可观测：内存 MeterRegistry、无 Prometheus 抓取端点；traceId 字段预留但未贯通——列为复赛计划。
- 协同推荐：含数处占位实现，依赖数据积累。
