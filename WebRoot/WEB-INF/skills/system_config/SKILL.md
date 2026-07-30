---
name: system_config
description: Get or update DocSystem configuration (admin only)
category: system
version: 1.1.0
author: DocSys Team
permissions:
  - admin
tags: [system, config, admin, settings]
---

# System Config

View or modify DocSystem configuration. Requires admin permissions.

## Triggers

Config, 系统配置, 配置管理, system-config, 系统设置, 配置, system settings, admin config, 服务器配置

## Workflow

1. **Identify mode**: Determine whether user wants --get (read), --set (write), or --list.
2. **Parse parameters**: Extract key and value from user input.
3. **⚠️⚠️ Confirm before --set**: Show key and new value → warn "This change affects all users immediately." Ask "Type the new value to confirm: [value]." If user does not type the exact value → stop.
4. **Call CLI**: `docsys system-config [--get <key>] [--set <key> <value>] [--list]`
5. **Present result**: Show current/new value. If "Invalid key" → suggest running `--list` to see valid keys.
6. **Post-change**: Note that the change takes effect immediately or after restart depending on the key.

## CLI Command

```bash
docsys system-config [--get <key>] [--set <key> <value>] [--list]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| --get | flag | no | Get configuration (all or specific key) |
| --set | flag | no | Set a configuration value |
| --list | flag | no | List all configurable keys |
| key | string | conditional | Config key (required for --get or --set) |
| value | string | conditional | Config value (required for --set) |

## Examples

### Example 1
User: "show system config"
Skill triggers → calls: `docsys system-config`
Output: All configuration values

### Example 2
User: "get the default AI model setting"
Skill triggers → calls: `docsys system-config --get ai.default.model`
Output: Current default model value

### Example 3
User: "set max upload size to 200"
Skill triggers → Step 1: --set mode, key=max.upload.size, value=200 → Step 3: confirm "Type '200' to apply: max.upload.size = 200" → user types 200 → calls: `docsys system-config --set max.upload.size 200`
Output: Config updated

### Example 4 (--list)
User: "what config options are available"
Skill triggers → Step 1: --list mode → calls: `docsys system-config --list`
Output: List of all configurable keys and current values

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| key | string | Configuration key |
| value | string | Current or new value |
| scope | string | Effect scope: global / per-repo / session |

**Success --list (EN):** `System Configuration:\n🔧 {key} = {value} ({scope})\n...`
**Success --list (中文):** `系统配置:\n🔧 {key} = {value}（作用域: {scope}）\n...`
**Success --get (EN):** `🔧 {key} = {value}`
**Success --get (中文):** `🔧 {key} = {value}`
**Success --set (EN):** `✅ {key} updated: {old_value} → {new_value}. Takes effect immediately.`
**Success --set (中文):** `✅ 配置已更新：{key}: {旧值} → {新值}。立即生效。`
**Error (EN):** `❌ {error_message}`
**Error (中文):** `❌ 配置失败：{错误信息}`

## Error Handling

| Error | Cause |
|-------|-------|
| Permission denied | Non-admin user |
| Invalid key | Unknown configuration key |
| Invalid value | Value doesn't match expected type |
| Config locked | Key is read-only |

See [references/](references/) for related skills and API docs.
