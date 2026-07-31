package com.DocSystem.agent.tool;

import com.DocSystem.agent.client.DocSysClient;
import com.DocSystem.agent.search.WebSearchResult;
import com.DocSystem.agent.search.WebSearchService;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * T8.4 护栏：网络搜索工具（web_search）+ WebSearchService。
 * 纯 Java 自包含测试（main 入口），本地 HTTP 服务器模拟搜索端点，无 Spring/外网依赖。
 *
 * 覆盖：
 *  - DuckDuckGo HTML 格式解析（标题/链接/摘要 + uddg 重定向解码）
 *  - JSON 端点格式解析（title/url/snippet 及包装 data[]）
 *  - 失败安全回退（端点 500 / 连接拒绝 / 超时 → 清晰错误，不抛异常）
 *  - 空结果 → 提示未找到
 *  - 工具集成（web_search 工具 + registry 注册 + maxResults 上限）
 */
public class TestWebSearchTool {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        testHtmlParse();
        testJsonParse();
        testJsonWrappedParse();
        testHttp500Fallback();
        testConnectionRefusedFallback();
        testEmptyResults();
        testToolIntegration();
        testMaxResultsLimit();
        System.out.println("\n======== TestWebSearchTool: " + pass + " passed, " + fail + " failed ========");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            pass++;
            System.out.println("[PASS] " + name);
        } else {
            fail++;
            System.out.println("[FAIL] " + name);
        }
    }

    /** 启动一个返回固定 body 的本地 HTTP 服务器 */
    private static HttpServer startServer(int statusCode, String body) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/search", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static String endpointOf(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/search?q=";
    }

    private static final String DUMMY_HTML =
            "<html><body>"
            + "<a rel=\"nofollow\" class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fpage1&amp;rut=abc\">"
            + "Java 8 流式处理 <b>示例</b></a>"
            + "<a class=\"result__snippet\" href=\"//duckduckgo.com/l/?uddg=...\">这是摘要一 &amp; 更多</a>"
            + "<a rel=\"nofollow\" class=\"result__a\" href=\"https://example.org/page2\">第二个结果</a>"
            + "<a class=\"result__snippet\" href=\"#\">摘要二</a>"
            + "</body></html>";

    private static void testHtmlParse() throws Exception {
        HttpServer server = startServer(200, DUMMY_HTML);
        try {
            WebSearchService svc = new WebSearchService(endpointOf(server), 3000);
            WebSearchService.SearchOutcome out = svc.search("java", 5);
            check("html: success", out.isSuccess());
            check("html: 2 results", out.results != null && out.results.size() == 2);
            if (out.results.size() >= 1) {
                WebSearchResult r0 = out.results.get(0);
                check("html: title stripped", r0.title.contains("流式处理") && !r0.title.contains("<b>"));
                check("html: uddg decoded url", "https://example.com/page1".equals(r0.url));
                check("html: snippet stripped", r0.snippet.contains("这是摘要一") && r0.snippet.contains("&"));
            }
            if (out.results.size() >= 2) {
                check("html: plain url kept", "https://example.org/page2".equals(out.results.get(1).url));
            }
        } finally {
            server.stop(0);
        }
    }

    private static void testJsonParse() throws Exception {
        String body = "[{\"title\":\"T1\",\"url\":\"http://a.com\",\"snippet\":\"S1\"},"
                + "{\"title\":\"T2\",\"link\":\"http://b.com\",\"description\":\"S2\"}]";
        HttpServer server = startServer(200, body);
        try {
            WebSearchService svc = new WebSearchService(endpointOf(server), 3000);
            WebSearchService.SearchOutcome out = svc.search("json", 5);
            check("json: success", out.isSuccess());
            check("json: 2 results", out.results != null && out.results.size() == 2);
            if (out.results.size() >= 2) {
                check("json: url field", "http://a.com".equals(out.results.get(0).url));
                check("json: link field fallback", "http://b.com".equals(out.results.get(1).url));
            }
        } finally {
            server.stop(0);
        }
    }

    private static void testJsonWrappedParse() throws Exception {
        String body = "{\"data\":[{\"title\":\"W1\",\"url\":\"http://w.com\",\"snippet\":\"WS\"}]}";
        HttpServer server = startServer(200, body);
        try {
            WebSearchService svc = new WebSearchService(endpointOf(server), 3000);
            WebSearchService.SearchOutcome out = svc.search("wrapped", 5);
            check("json wrapped: 1 result", out.isSuccess() && out.results != null && out.results.size() == 1
                    && "W1".equals(out.results.get(0).title));
        } finally {
            server.stop(0);
        }
    }

    private static void testHttp500Fallback() throws Exception {
        HttpServer server = startServer(500, "oops");
        try {
            WebSearchService svc = new WebSearchService(endpointOf(server), 3000);
            WebSearchService.SearchOutcome out = svc.search("err", 5);
            check("http 500 -> error (not exception)", !out.isSuccess() && out.error != null
                    && out.error.contains("500"));
        } finally {
            server.stop(0);
        }
    }

    private static void testConnectionRefusedFallback() throws Exception {
        // 找一个必然关闭的端口（先起一个再停掉）
        HttpServer tmp = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = tmp.getAddress().getPort();
        tmp.stop(0);
        WebSearchService svc = new WebSearchService("http://127.0.0.1:" + port + "/search?q=", 1500);
        WebSearchService.SearchOutcome out = svc.search("refused", 5);
        check("connection refused -> error (not exception)", !out.isSuccess() && out.error != null);
    }

    private static void testEmptyResults() throws Exception {
        HttpServer server = startServer(200, "<html><body>无结果</body></html>");
        try {
            WebSearchService svc = new WebSearchService(endpointOf(server), 3000);
            WebSearchService.SearchOutcome out = svc.search("empty", 5);
            check("empty html -> success with 0 results", out.isSuccess() && out.results.isEmpty());
        } finally {
            server.stop(0);
        }
    }

    private static void testToolIntegration() throws Exception {
        HttpServer server = startServer(200, DUMMY_HTML);
        try {
            WebSearchService svc = new WebSearchService(endpointOf(server), 3000);
            ToolDefinition tool = DocSysToolFactory.webSearch(svc);
            check("web_search registered type", tool != null && "web_search".equals(tool.name));
            check("web_search is read-only", !tool.isWrite && !tool.needsConfirm);

            JSONObject args = new JSONObject();
            args.put("query", "java");
            ToolResult r = tool.executor.execute(args);
            check("tool execute success", r.success);
            check("tool result contains title", r.summary.contains("流式处理"));
            check("tool result contains decoded url", r.summary.contains("https://example.com/page1"));

            // 空 query → 错误
            JSONObject bad = new JSONObject();
            bad.put("query", "  ");
            check("tool empty query -> error", !tool.executor.execute(bad).success);

            // registry 集成：createFullRegistry(..., webSearch) 注册
            DocSysClient client = new DocSysClient("http://localhost:9999/");
            ToolRegistry reg = DocSysToolFactory.createFullRegistry(client, null, null, svc);
            check("registry has web_search", reg.find("web_search") != null);
            ToolRegistry regNoWeb = DocSysToolFactory.createFullRegistry(client, null, null, null);
            check("web_search absent when svc null", regNoWeb.find("web_search") == null);
        } finally {
            server.stop(0);
        }
    }

    private static void testMaxResultsLimit() throws Exception {
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 1; i <= 12; i++) {
            sb.append("<a class=\"result__a\" href=\"http://e.com/" + i + "\">标题" + i + "</a>");
        }
        sb.append("</html>");
        HttpServer server = startServer(200, sb.toString());
        try {
            WebSearchService svc = new WebSearchService(endpointOf(server), 3000);
            WebSearchService.SearchOutcome out = svc.search("many", 20); // 请求 20，上限 10
            check("maxResults capped at 10", out.isSuccess() && out.results.size() == 10);
        } finally {
            server.stop(0);
        }
    }
}
