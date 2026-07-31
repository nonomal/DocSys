package com.DocSystem.agent.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网络搜索服务（T8.4）。
 *
 * <p>对可配置的搜索端点发起 HTTP 请求并解析结果：
 * <ul>
 *   <li>默认端点：DuckDuckGo HTML（无需 API key）——解析 {@code result__a}（标题/链接）与
 *       {@code result__snippet}（摘要），并还原其 {@code //duckduckgo.com/l/?uddg=} 重定向链接。</li>
 *   <li>若端点返回 JSON（以 {@code [} 或 {@code \{} 开头）：尝试通用字段
 *       （title / url|link / snippet|description|content），兼容常见搜索 API 返回。</li>
 * </ul>
 * 失败安全：网络错误/超时/解析失败一律不抛异常，返回空列表 + 错误信息，由上层转成清晰错误结果。</p>
 */
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

    /** 默认搜索端点（DuckDuckGo HTML，无需 API key） */
    public static final String DEFAULT_ENDPOINT = "https://html.duckduckgo.com/html/?q=";

    /** 默认超时（毫秒） */
    public static final long DEFAULT_TIMEOUT_MS = 8000;

    private final String endpoint;
    private final long timeoutMs;
    private final OkHttpClient httpClient;

    private static final Pattern HTML_TITLE_PATTERN =
            Pattern.compile("<a[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                    Pattern.DOTALL);
    private static final Pattern HTML_SNIPPET_PATTERN =
            Pattern.compile("<a[^>]*class=\"[^\"]*result__snippet[^\"]*\"[^>]*>(.*?)</a>",
                    Pattern.DOTALL);

    public WebSearchService() {
        this(DEFAULT_ENDPOINT, DEFAULT_TIMEOUT_MS);
    }

    public WebSearchService(String endpoint, long timeoutMs) {
        this.endpoint = (endpoint == null || endpoint.isEmpty()) ? DEFAULT_ENDPOINT : endpoint;
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(this.timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(this.timeoutMs, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .build();
    }

    /** 搜索结果：results 为解析出的条目；error 为失败原因（成功时 null） */
    public static class SearchOutcome {
        public final List<WebSearchResult> results;
        public final String error;
        public SearchOutcome(List<WebSearchResult> results, String error) {
            this.results = results;
            this.error = error;
        }
        public boolean isSuccess() { return error == null; }
    }

    /**
     * 执行搜索。
     *
     * @return 成功 → results 非空或空（无结果）；失败 → error 非空（失败安全，不抛异常）
     */
    public SearchOutcome search(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchOutcome(new ArrayList<WebSearchResult>(), "搜索关键词不能为空");
        }
        int limit = Math.max(1, Math.min(maxResults <= 0 ? 5 : maxResults, 10));

        String url;
        try {
            String encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8");
            url = endpoint + encoded;
        } catch (Exception e) {
            return new SearchOutcome(new ArrayList<WebSearchResult>(), "编码搜索词失败: " + e.getMessage());
        }

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) DocSysAgent/1.0")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return new SearchOutcome(new ArrayList<WebSearchResult>(),
                        "搜索端点返回 HTTP " + response.code());
            }
            String body = response.body() != null ? response.body().string() : "";
            if (body == null || body.isEmpty()) {
                return new SearchOutcome(new ArrayList<WebSearchResult>(), "搜索端点返回空内容");
            }
            List<WebSearchResult> results = parse(body, limit);
            return new SearchOutcome(results, null);
        } catch (java.net.SocketTimeoutException e) {
            return new SearchOutcome(new ArrayList<WebSearchResult>(), "搜索超时（" + timeoutMs + "ms）");
        } catch (Exception e) {
            log.warn("WebSearchService.search failed: query={}, err={}", query, e.getMessage());
            return new SearchOutcome(new ArrayList<WebSearchResult>(), "搜索失败: " + e.getMessage());
        }
    }

    /** 解析端点响应（HTML 或 JSON） */
    private List<WebSearchResult> parse(String body, int limit) {
        String trimmed = body.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return parseJson(trimmed, limit);
        }
        return parseHtml(body, limit);
    }

    /** 解析 DuckDuckGo HTML 结果页 */
    private List<WebSearchResult> parseHtml(String html, int limit) {
        List<WebSearchResult> results = new ArrayList<>();
        try {
            Matcher titleMatcher = HTML_TITLE_PATTERN.matcher(html);
            Matcher snippetMatcher = HTML_SNIPPET_PATTERN.matcher(html);
            while (titleMatcher.find() && results.size() < limit) {
                String href = titleMatcher.group(1).trim();
                String title = stripHtml(titleMatcher.group(2)).trim();
                if (title.isEmpty()) {
                    continue;
                }
                String url = normalizeUrl(href);
                // 取对应的摘要（尽力对齐：后续 snippetMatcher.find()）
                String snippet = "";
                if (snippetMatcher.find()) {
                    snippet = stripHtml(snippetMatcher.group(1)).trim();
                }
                results.add(new WebSearchResult(title, url, snippet));
            }
        } catch (Exception e) {
            log.warn("WebSearchService.parseHtml failed: {}", e.getMessage());
        }
        return results;
    }

    /** 解析 JSON 端点响应（兼容 title/url|link/snippet|description|content 字段） */
    private List<WebSearchResult> parseJson(String json, int limit) {
        List<WebSearchResult> results = new ArrayList<>();
        try {
            JSONArray items = null;
            try {
                items = JSON.parseArray(json);
            } catch (Exception ignored) {
                JSONObject obj = JSON.parseObject(json);
                if (obj != null) {
                    // 常见包装：data / results / items / organic
                    for (String key : new String[]{"data", "results", "items", "organic", "hits"}) {
                        Object v = obj.get(key);
                        if (v instanceof JSONArray) { items = (JSONArray) v; break; }
                    }
                    if (items == null) { items = new JSONArray(); }
                }
            }
            if (items == null) { return results; }
            for (int i = 0; i < items.size() && results.size() < limit; i++) {
                JSONObject it = items.getJSONObject(i);
                if (it == null) { continue; }
                String title = firstString(it, "title", "name");
                String url = firstString(it, "url", "link", "href");
                String snippet = firstString(it, "snippet", "description", "content", "abstract");
                if (title == null && url == null) { continue; }
                results.add(new WebSearchResult(
                        title != null ? title : "",
                        url != null ? url : "",
                        snippet != null ? snippet : ""));
            }
        } catch (Exception e) {
            log.warn("WebSearchService.parseJson failed: {}", e.getMessage());
        }
        return results;
    }

    private static String firstString(JSONObject obj, String... keys) {
        for (String k : keys) {
            Object v = obj.get(k);
            if (v != null && !v.toString().isEmpty()) {
                return v.toString();
            }
        }
        return null;
    }

    /** 还原 DuckDuckGo 的 //duckduckgo.com/l/?uddg=<encoded> 重定向链接 */
    private static String normalizeUrl(String href) {
        if (href == null || href.isEmpty()) {
            return "";
        }
        try {
            int idx = href.indexOf("uddg=");
            if (idx >= 0) {
                String encoded = href.substring(idx + 5);
                int amp = encoded.indexOf('&');
                if (amp >= 0) { encoded = encoded.substring(0, amp); }
                return URLDecoder.decode(encoded, "UTF-8");
            }
            // 协议相对链接补全
            if (href.startsWith("//")) {
                return "https:" + href;
            }
        } catch (Exception ignored) {}
        return href;
    }

    /** 去除 HTML 标签与实体 */
    private static String stripHtml(String s) {
        if (s == null) { return ""; }
        String t = s.replaceAll("<[^>]+>", " ");
        t = t.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&")
              .replaceAll("&lt;", "<").replaceAll("&gt;", ">")
              .replaceAll("&quot;", "\"").replaceAll("&#39;", "'");
        return t.replaceAll("\\s+", " ").trim();
    }
}
