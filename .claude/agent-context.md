# Agent 开发上下文（给 Claude / 开发助手）

> 这是**工作上下文**，不是产品文档。产品文档在 `docs/agent/`，模块正文在
> `src/com/DocSystem/agent/README.md`。每次开工先读这三处 + 本文件。

---

## 工作目录（重要）

- **在 `D:\Dev\DocSys` 工作。** Agent 已合并进 DocSys。
- `D:\Dev\DocSysAgent` 只作历史来源，**不改代码、不作构建目标**。
- 详见 `docs/agent/migration-notes.md`。

---

## 环境事实

- 语言：Java 8。编译器：`C:\Program Files\Java\jdk1.8.0_162\bin\javac`。
- 构建：无 Maven；jar 在 `WebRoot/WEB-INF/lib`，class 输出到 `WebRoot/WEB-INF/classes`。
- 编译命令：
  ```bash
  "C:\Program Files\Java\jdk1.8.0_162\bin\javac" -encoding UTF-8 \
    -cp "WebRoot/WEB-INF/classes;WebRoot/WEB-INF/lib/*" \
    -d WebRoot/WEB-INF/classes <改动的 .java>
  ```
- Mapper XML 改动后要同步到 `WebRoot/WEB-INF/classes/mapper/`。
- 前端只动 `WebRoot/web/agent/index.html`。
- 部署 / 启动顺序见记忆 `deploy-procedure`（用 docsys_start.bat，不要用 docsys_restart.bat）。

---

## 关键代码坐标

| 事项 | 位置 |
|---|---|
| REST/SSE 入口 | `src/com/DocSystem/agent/controller/AgentController.java`（`/agent`） |
| 编排 | `src/com/DocSystem/agent/orchestrator/MainAgent.java` |
| 执行 | `src/com/DocSystem/agent/orchestrator/SubAgent.java` |
| LLM 服务 | `src/com/DocSystem/agent/llm/LLMService.java` |
| 请求级模型配置 | `src/com/DocSystem/agent/llm/ResolvedLlmConfig.java` |
| 模型解析/CRUD | `src/com/DocSystem/agent/llm/UserLlmModelService.java` |
| 建表 | `src/com/DocSystem/agent/config/DatabaseInitializer.java` |

---

## 待办（代码，未做）

### [关键 1] LLM 模型选择未贯通自然语言链路
自然语言查询链路全程不带用户所选模型，恒用系统默认：
```
AgentController.runCommand(cmd, username, jsessionid)   ← 需加 ResolvedLlmConfig
  └ MainAgent.process(cmd, ctx, info, client)           ← 需加 ResolvedLlmConfig 重载
      └ executeSubTasks(decomposition, ctx, client)     ← 需加参数
          └ SubAgent.execute(subTask, ctx, client, null)← 第4参当前为 null
              └ handleChat / handleSearchAndAnswer / handleGenerateSummary
                  └ LLMService.chat(msg, sid)           ← 需新增 chat(msg, sid, ResolvedLlmConfig) 无状态重载
```
- `chat ` 前缀的 `isAiChat` 分支已正确用 `resolvedLlm`（可参照 `streamChat(msg,session,resolved)`）。
- 新重载必须**只用局部变量**，不得调用 `refreshFromSystemConfig` / `applyConfig`（会写共享字段，有并发竞态）。
- 旧无参签名保留给 `/execute`、`executeSmart`（传 null 回退默认，等价现状）。

### [关键 2] 系统提示词固化身份
`LLMService.chat()`（约 L237-240）与 `streamChat()`（约 L512）的 system prompt 强设
"You are DocSys AI Assistant, a professional document management system assistant..."，
模型无法如实回答自身身份。弱化为仅场景背景，例如：
`"You are a helpful AI assistant in DocSys document management system."`

### [部署校验]
- `WebRoot/WEB-INF/classes/mapper/UserCustomLlmModelRepositoryMapper.xml` 是否已从 `src/mapper/` 部署。
- 所有改动 `.java` 用 JDK 1.8 编译通过。

---

## 已完成（供参考，勿重做）

- 用户可选 + 自定义 LLM 模型全栈：DB 表 `user_custom_llm_models`、entity、repository、
  mapper XML、`UserLlmModelService`、`AgentController` 的 models CRUD 端点、前端下拉 + 管理弹窗。
- `AgentController.stream()` 的 `isAiChat` 分支已正确贯通 `resolvedLlm`。
- 技能存储目录可配置（系统设置），初始化按技能子目录逐个复制、已存在不覆盖。

> 注意历史教训：不要反复重读已确认的大文件（如 SubAgent.java 1500+ 行）导致上下文膨胀；
> 需要签名时定点读对应方法段即可。
