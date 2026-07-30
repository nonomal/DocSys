---
name: java-expert
description: Optimize and fix Java source code while preserving all functional interfaces, variables, classes, and method signatures
category: code
version: 1.0.0
author: DocSys Team
permissions:
  - read
  - write
tags: [java, code, optimize, fix, refactor, compile]
---

# Java Expert

Analyze, optimize, and fix Java source code. Preserve ALL functional interfaces, variable names, class names, method signatures, and API contracts — only improve implementation quality, correctness, and performance.

## Triggers

优化 java, 修复 java, java 代码, java 错误, java 问题, 编译错误, fix java, optimize java, java compile error, java bug, java 代码优化, java 重构, java refactor, java performance, java 性能, java 语法错误, java syntax error, java exception, java 异常, java class, java 方法, java 脚本

## Workflow

1. **Read the source code** — Read all Java files fully, do not summarize or skip content
2. **Identify the problem** — compilation error, runtime exception, logic bug, performance issue, or style problem
3. **Check unchanged contracts** — Confirm all public/protected method signatures, class names, field names, and interface implementations are preserved
4. **Apply fix** — Minimal, focused change that fixes the root cause without altering the API surface
5. **Verify compilation** — If build tool available, verify `mvn compile` or `javac` passes with no errors

## Golden Rules

- **NEVER change** method signatures (name, parameters, return type, throws clauses)
- **NEVER rename** public/protected fields, variables, or class names
- **NEVER remove** existing interface implementations or superclass relationships
- **DO change** method body implementation, variable scope, algorithm, error handling
- **DO add** null checks, bounds checks, logging, and defensive guards
- **DO fix** logic errors, off-by-one bugs, resource leaks, concurrency issues
- **DO improve** performance (loops, data structures, caching) while preserving semantics

## Common Fixes

| Category | Symptom | Fix |
|----------|---------|-----|
| Compilation error | syntax error, type mismatch | Correct syntax, add cast, fix generics |
| Null pointer | NPE on method call | Add null check guard, Objects.requireNonNull |
| Resource leak | stream/connection not closed | Use try-with-resources |
| Concurrency | race condition | Add synchronized, volatile, or concurrent collection |
| Logic bug | wrong output | Fix algorithm, correct condition, fix boundary |
| Performance | slow loop, O(n²) | Use better data structure, algorithm optimization |
| Missing brace | unclosed block | Close all blocks properly |
| Import error | package not found | Fix import path, use fully-qualified name |

## CLI Command (optional)

```bash
# Compile check
mvn compile -f /path/to/pom.xml

# Single file compile
javac -cp "lib/*" src/Main.java

# Run tests
mvn test -f /path/to/pom.xml
```

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| file_path | Yes | Absolute path to the Java file to fix |
| problem_type | No | "compile" / "runtime" / "logic" / "performance" / "style" |
| preserve_contracts | Yes | Must always be true — never change API surface |

## Examples

### Example 1 (编译错误修复)
User: "Fix the compilation error in MainAgent.java"
Problem: The `switch` statement at line 403 is missing a closing brace `}`
Skill triggers → reads MainAgent.java → identifies brace mismatch
Fix: Add missing `}` before line 670
Output: Fixed `MainAgent.java:670` — added closing `}` for switch statement. Compilation passes.

### Example 2 (空指针异常修复)
User: "java 代码有空指针异常"
Problem: `redisTemplate` may be null but is used without null check
Skill triggers → reads RedisSessionService.java → finds method using redisTemplate
Fix: Add `if (redisTemplate == null) { return; }` guard at method start
Output: Fixed `RedisSessionService.java` — added null guard for redisTemplate field.

### Example 3 (逻辑错误修复)
User: "修复 list_docs 的逻辑，查询条件写错了"
Problem: List docs pattern matching incorrectly excludes results
Skill triggers → reads MainAgent.java → finds pattern: `nluQuery.contains("里的")` too broad
Fix: Narrow pattern to `nluQuery.contains("里的文件")` or `nluQuery.contains("目录")`
Output: Fixed `MainAgent.java:586` — refined list_docs pattern matching logic.

### Example 4 (性能优化)
User: "优化这个循环的性能"
Problem: Nested loop O(n²) when looking up by key
Skill triggers → reads code → identifies HashMap lookup could replace loop
Fix: Use Map.get(key) instead of iterating all entries
Output: Fixed — replaced O(n²) loop with O(1) HashMap lookup. Performance improved.

### Example 5 (语法错误)
User: "Maven 编译报语法错误"
Problem: Duplicate variable declaration in method scope
Skill triggers → runs `mvn compile` → sees "variable already defined" error
Fix: Rename inner variable or merge logic to use single variable
Output: Fixed — removed duplicate variable declaration. Compilation passes.

### Example 6 (并发安全)
User: "这个多线程代码有线程安全问题"
Problem: Shared mutable state accessed without synchronization
Skill triggers → reads code → finds counter/sum shared across threads
Fix: Add `synchronized` keyword or use `AtomicInteger` / `ConcurrentHashMap`
Output: Fixed — added thread-safe concurrent access pattern.

## Output Format

Success (English):
```
[java-expert] Fixed <file_path>

Problem: <brief description of the issue>
Root Cause: <why it happened>
Fix Applied: <what was changed>
Verification: <mvn compile passes / no errors>

Before:
<code snippet before fix>

After:
<code snippet after fix>
```

Success (中文):
```
[java-expert] 已修复 <file_path>

问题: <问题简要描述>
根本原因: <为什么会发生>
修复内容: <具体修改了什么>
验证结果: <编译通过 / 无错误>

修复前:
<code snippet>

修复后:
<code snippet>
```

| Field | English | 中文 |
|-------|---------|------|
| problem | "Compilation error" | "编译错误" |
| root_cause | "Missing closing brace" | "缺少闭合大括号" |
| fix_applied | "Added } at line 670" | "在第670行添加了}" |
| verification | "mvn compile passed" | "编译通过" |

Error — File not found: "Java source file not found at <path>. Please provide a valid absolute path."
Error — No write permission: "Cannot modify file — no write permission. Check file ownership."
Error — API contract violated: "⚠️ REFUSE: This fix would change a public method signature (<class.method>). Only implementation changes allowed."

## Error Handling

| Error | Cause | Recovery |
|-------|-------|----------|
| File not found | Path doesn't exist | Ask user for correct absolute path |
| No write permission | OS permission denied | Suggest using sudo or checking file ownership |
| Build not available | No mvn/javac found | Verify fix manually by code inspection |
| API contract violation | Attempted to rename method/field | Reject fix, explain golden rule |

See [references/](references/) for Java best practices, common patterns, and style guides.
