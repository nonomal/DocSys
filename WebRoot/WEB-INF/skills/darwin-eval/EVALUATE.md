# Darwin-Eval: Real Subagent Evaluation Framework

真实测评框架——通过独立子 agent 对比 WITH-SKILL vs BASELINE 实际输出质量。

## 核心原则

1. **不自己评自己** — 主 agent 提出优化，子 agent 独立打分
2. **真实场景测试** — 每个 skill 准备 2-3 个典型用户场景
3. **棘轮机制** — 只保留有真实提升的改动
4. **结果可追溯** — 所有评分记录到 results.tsv

## 评估流程

```
┌─────────────────────────────────────────────────────────┐
│ Step 1: 读取 SKILL.md + 提取测试场景                       │
│   → 识别 skill 的核心用途、CLI 命令、参数、触发词           │
│   → 构造 2-3 个典型用户 prompt                            │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Step 2: BASELINE 子 agent（不带 SKILL.md）                 │
│   → 给子 agent 同样的用户 prompt                          │
│   → 不提供任何 skill 文件                                 │
│   → 记录输出质量                                          │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Step 3: WITH-SKILL 子 agent（带 SKILL.md）               │
│   → 给子 agent 同样的用户 prompt + SKILL.md               │
│   → 要求按 skill 的 CLI 命令和流程执行                     │
│   → 记录输出质量                                          │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Step 4: 评分计算                                          │
│   → 8 维度独立打分（主 agent 执行）                        │
│   → 实测表现：WITH > BASELINE 的程度                       │
│   → 结构维度：静态分析 + 与 DocSysCLI.java 对比             │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
              results.tsv 记录 + 决定 keep/revert
```

## 评分维度

| # | 维度 | 权重 | 评估方式 |
|---|------|------|---------|
| 1 | Frontmatter | 8 | 静态检查 |
| 2 | 工作流清晰度 | 15 | 静态分析 |
| 3 | 边界条件覆盖 | 10 | 错误场景模拟 |
| 4 | 检查点设计 | 7 | 决策点覆盖 |
| 5 | 指令具体性 | 15 | 命令完整性 |
| 6 | 资源整合度 | 5 | 引用正确性 |
| 7 | 整体架构 | 15 | 静态结构分析 |
| 8 | 实测表现 | 25 | WITH vs BASELINE 对比 |

## 实测评分细则（维度8）

对每个测试 prompt 评分：

| 分数 | 含义 |
|------|------|
| 1-3 | SKILL 反而让输出更差或无提升 |
| 4-5 | SKILL 轻微改善，baseline 接近 |
| 6-7 | SKILL 明显改善，CLI 命令基本正确 |
| 8-9 | SKILL 优秀，CLI 完整+错误处理到位 |
| 10 | 完美，超越 baseline 且无冗余 |

总分 = (维度1-7 加权平均) × 0.75 + 维度8 × 0.25

## CLI 正确性检查（关键！）

必须对照 DocSysCLI.java 验证：

```bash
# 检查 skill 中的 CLI 命令是否与 DocSysCLI.java 一致
grep -n "repos add\|doc get\|download\|upload" \
  DocSysAgent/src/main/java/com/docsys/agent/cli/DocSysCLI.java
```

常见错误：
- ❌ HTTP API 路径（POST /Repos/add.do）→ ✅ CLI 命令（docsys repos add）
- ❌ 错误的参数名或顺序
- ❌ 不存在的子命令

## 输出格式

每轮评估后生成：

```
SKILL: {skill_name}
═══════════════════════════════════════
维度 1-7 静态评分: {score_structural}/100
维度 8 实测评分:   {score_measured}/25

总分: {total}/100
改进前: {old_score}
改进后: {new_score}
状态: {'keep' if new > old else 'revert'}

CLI 正确性: {'✓' if cli_correct else '✗ ERROR: ...'}
主要问题:
  - {issue_1}
  - {issue_2}
建议改进:
  - {suggestion_1}
  - {suggestion_2}
```

## results.tsv 格式

```
timestamp	skill	old_score	new_score	status	cli_correct	eval_mode	main_issue
2026-04-16T10:00	create_repos	75	82	keep	✓	full_test	边界条件
2026-04-16T10:05	delete_doc	68	71	revert	✓	full_test	实测无提升
```
