---
name: banner
description: Display DocSys welcome banner, named banners, or list available banners
category: system
version: 1.2.0
author: DocSys Team
permissions:
  - read
tags: [system, banner, welcome, display, ascii, branding]
---

# Banner

Display the DocSys welcome banner, a named banner, or list all available banners.

## Triggers

Banner, 横幅, welcome, 欢迎信息, show banner, display welcome, 展示横幅, 欢迎页面, welcome message, 欢迎横幅, 展示欢迎页, 欢迎词, 横幅消息, 启动画面, 欢迎界面, display banner, show welcome screen, show the banner, what banners are available, list banners, 有什么横幅, 可用横幅, 查看横幅

## Workflow

1. **Parse intent**: Determine if user wants to (a) show a specific banner by name, (b) show the default welcome banner, or (c) list available banners.
   - "what banners" / "list" / "有哪些横幅" / "available banners" → intent = LIST
   - Specific name provided ("holiday banner", "节日横幅") → intent = NAMED
   - Default / no name → intent = DEFAULT
2. **Named banner**: If user names a banner → call `docsys banner [name]`.
3. **⚠️ If banner not found**: CLI returns "Banner not found" → call `docsys banner --list` (or try `docsys help` to find available banners) → show list to user → ask "Did you mean: [closest match]?"
4. **List banners**: If intent = LIST → call `docsys banner --list` or show all known banners. Common banners: welcome, docsys, default, holiday.
5. **Display**: Show banner content as-is. **Preserve all whitespace, line breaks, and ASCII art formatting.** Do not reformat, wrap, or truncate.
6. **Empty name**: If user passes empty string `""` → treat as intent = DEFAULT (show welcome banner).

## CLI Command

```bash
docsys banner [name]
docsys banner --list    # list available banners
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| name | string | no | Banner name (shows default if omitted). Empty string = default. |

## Examples

### Example 1 (default welcome)
User: "show the welcome banner"
Skill triggers → intent = DEFAULT → calls: `docsys banner`
Output: [ASCII welcome banner — preserve formatting exactly]

### Example 2 (named banner)
User: "display the holiday banner"
Skill triggers → intent = NAMED, name="holiday" → calls: `docsys banner holiday`
Output: [Holiday banner content — preserve formatting exactly]

### Example 3 (Chinese — default)
User: "横幅"
Skill triggers → intent = DEFAULT → calls: `docsys banner`
Output: 横幅显示内容

### Example 4 (list available banners)
User: "what banners are available"
Skill triggers → intent = LIST → Step 4: calls `docsys banner --list` (or shows known banners)
Output: Available banners: welcome (default), docsys, holiday, custom-1. To display: `docsys banner [name]`

### Example 5 (banner not found → recovery)
User: "show the summer-sale banner"
Skill triggers → name="summer-sale" → calls: `docsys banner summer-sale`
Output: Banner "summer-sale" not found.
Skill triggers → Step 3: recovery → "Available banners: welcome, docsys, holiday. Did you mean 'holiday'?" → user corrects or picks from list.

### Example 6 (empty string → default)
User: "banner" (with empty string argument)
Skill triggers → Step 6: empty string → intent = DEFAULT → calls: `docsys banner`
Output: Default welcome banner

## Output Format

| Field | Type | Description |
|-------|------|-------------|
| content | string | Raw banner text including whitespace/ASCII art/Unicode |
| name | string | Banner name used (or "default" if none) |
| format | string | Display format: ascii / unicode / html |

**Success (EN):** Display raw banner content as-is. Preserve all whitespace, line breaks, ASCII art.
**Success (中文):** 直接显示横幅内容，保留所有空白字符、换行符和ASCII art格式。
**Not found:** `❌ Banner "{name}" not found. Available: welcome, docsys, holiday. Did you mean 'holiday'?`
Error: "Banner not found" | "No banners available" | "Empty output"

## Error Handling

| Error | Cause |
|-------|-------|
| Banner not found | Named banner doesn't exist |
| No banners available | No banners configured |
| Empty output | Banner exists but returned nothing |

See [references/](references/) for related skills and API docs.
