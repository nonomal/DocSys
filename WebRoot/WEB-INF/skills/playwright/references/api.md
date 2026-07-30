# API Reference

## Playwright Browser Automation
- **Type**: Browser automation library (Playwright)
- **Auth**: N/A
- **Response**: Varies by operation (page load, screenshot, click result)

## Supported Operations
| Operation | Description |
|-----------|-------------|
| open | Navigate to URL |
| screenshot | Take page screenshot |
| click | Click element by selector |
| fill | Fill form field by selector |
| close | Close browser |

## Error Codes
| Error | Message |
|-------|---------|
| 400 | Missing URL or selector |
| 404 | Element not found |
| 504 | Navigation timeout |
