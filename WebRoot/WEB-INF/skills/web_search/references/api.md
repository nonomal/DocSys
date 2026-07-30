# API Reference

## Web Search
- **Endpoint**: External API (search engines: Baidu, Google, Bing)
- **Auth**: Depends on engine
- **Response**: JSON array of search results with title, URL, description

## Error Codes
| Error | Message |
|-------|---------|
| 400 | Empty query |
| 429 | Rate limit exceeded |
| 500 | Search engine blocked or unavailable |
