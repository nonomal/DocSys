package com.DocSystem.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 智能自适应部署服务
 *
 * 自动探索当前环境，尝试多种方式获取 MySQL 和 DocSystem 配置。
 * 如果自动探索失败，提供用户友好的配置引导。
 *
 * 功能：
 * 1. 自动发现 MySQL 服务（端口 3306, 3307, 3308, 3309）
 * 2. 自动发现 DocSystem 服务（端口 8100, 8200, 8080, 8000）
 * 3. 自动尝试多种连接策略
 * 4. 提供配置失败时的用户引导
 */
@Component
public class AdaptiveDeploymentService {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveDeploymentService.class);

    /** 常见的 MySQL 端口 */
    private static final int[] MYSQL_PORTS = {3306, 3307, 3308, 3309};

    /** 常见的 DocSystem/Tomcat 端口 */
    private static final int[] DOCSYS_PORTS = {8100, 8200, 8080, 8000, 8090};

    /** 常见的 DocSystem context paths */
    private static final String[] DOCSYS_CONTEXTS = {"/DocSystem", "/docsystem", "/DocSys", "/ROOT"};

    /** 可能的 jdbc.properties 位置 */
    private static final String[] JDBC_PROPERTIES_PATHS = {
        "F:/gy/docsys/tomcat/webapps/DocSystem/WEB-INF/classes/jdbc.properties",
        "F:/gy/docsys-win-2.02.80/docsys/tomcat/webapps/DocSystem/WEB-INF/classes/jdbc.properties",
        "F:/gy/docsys/tomcat/webapps/DocSystem/WEB-INF/classes/jdbc.properties",
        "../DocSystem/WEB-INF/classes/jdbc.properties",
        "C:/tomcat9/apache-tomcat-9.0.117/webapps/DocSystem/WEB-INF/classes/jdbc.properties"
    };

    /** 探索结果 */
    public static class DiscoveryResult {
        public boolean mysqlFound;
        public String mysqlHost;
        public int mysqlPort;
        public String mysqlDatabase;
        public String mysqlUsername;
        public String mysqlPassword;

        public boolean docSysFound;
        public String docSysUrl;
        public String docSysContext;
        public String docSysUsername;
        public String docSysPassword;
        public boolean docSysLoginSuccess;

        public List<String> warnings = new ArrayList<>();
        public List<String> suggestions = new ArrayList<>();

        public boolean isFullyConfigured() {
            return mysqlFound && docSysFound && docSysLoginSuccess;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 环境探索结果 ===\n");
            sb.append("MySQL: ").append(mysqlFound ? "✅ 发现" : "❌ 未发现");
            if (mysqlFound) {
                sb.append(" (").append(mysqlHost).append(":").append(mysqlPort).append(")");
            }
            sb.append("\n");
            sb.append("DocSystem: ").append(docSysFound ? "✅ 发现" : "❌ 未发现");
            if (docSysFound) {
                sb.append(" (").append(docSysUrl).append(")");
            }
            sb.append("\n");
            sb.append("登录验证: ").append(docSysLoginSuccess ? "✅ 成功" : "❌ 失败");
            sb.append("\n");
            return sb.toString();
        }
    }

    /**
     * 执行完整的环境自适应探索
     */
    public DiscoveryResult discoverEnvironment() {
        log.info("=== 开始环境自适应探索 ===");
        DiscoveryResult result = new DiscoveryResult();

        // 步骤1: 探索 MySQL
        discoverMySQL(result);

        // 步骤2: 探索 DocSystem
        discoverDocSystem(result);

        // 步骤3: 尝试 DocSystem 登录
        tryDocSysLogin(result);

        // 步骤4: 生成建议
        generateSuggestions(result);

        log.info("=== 环境探索完成 ===");
        log.info(result.getSummary());

        return result;
    }

    /**
     * 探索 MySQL 服务
     */
    private void discoverMySQL(DiscoveryResult result) {
        log.info("探索 MySQL 服务...");

        // 策略1: 尝试读取 jdbc.properties
        Properties props = loadJdbcProperties();
        if (props != null) {
            String url = props.getProperty("jdbc.url");
            if (url != null && !url.isEmpty()) {
                log.info("从 jdbc.properties 读取到: {}", url);
                if (testConnection(url, props)) {
                    parseJdbcUrl(url, result);
                    result.mysqlUsername = props.getProperty("jdbc.username", "root");
                    result.mysqlPassword = props.getProperty("jdbc.password", "");
                    result.mysqlFound = true;
                    result.warnings.add("MySQL 配置来自 jdbc.properties");
                    return;
                }
            }
        }

        // 策略2: 尝试常见端口
        for (int port : MYSQL_PORTS) {
            for (String host : Arrays.asList("localhost", "127.0.0.1", "localhost.localdomain")) {
                for (String database : Arrays.asList("docsystem", "docsystem1", "docsys_agent", "mysql")) {
                    String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&characterEncoding=utf8";
                    log.debug("尝试连接: {}", url);
                    if (testConnection(url, null)) {
                        result.mysqlFound = true;
                        result.mysqlHost = host;
                        result.mysqlPort = port;
                        result.mysqlDatabase = database;
                        result.mysqlUsername = "root";
                        result.mysqlPassword = "";
                        log.info("✅ MySQL 发现成功: {}:{}", host, port);
                        return;
                    }
                }
            }
        }

        result.warnings.add("MySQL 服务未自动发现");
    }

    /**
     * 探索 DocSystem 服务
     */
    private void discoverDocSystem(DiscoveryResult result) {
        log.info("探索 DocSystem 服务...");

        for (int port : DOCSYS_PORTS) {
            for (String context : DOCSYS_CONTEXTS) {
                String url = "http://localhost:" + port + context;
                log.debug("尝试访问: {}", url);

                if (testHttpEndpoint(url + "/User/login.do")) {
                    result.docSysFound = true;
                    result.docSysUrl = url;
                    result.docSysContext = context;
                    log.info("✅ DocSystem 发现成功: {}", url);
                    return;
                }
            }
        }

        result.warnings.add("DocSystem 服务未自动发现");
    }

    /**
     * 尝试 DocSystem 登录
     */
    private void tryDocSysLogin(DiscoveryResult result) {
        if (!result.docSysFound) {
            return;
        }

        log.info("尝试 DocSystem 登录验证...");

        // 尝试常见凭据
        String[][] credentials = {
            {"admin", "123456"},
            {"admin", "admin123"},
            {"admin", "admin2026"},
            {"root", "root"},
            {"admin", ""}
        };

        for (String[] creds : credentials) {
            String username = creds[0];
            String password = creds[1];

            if (testDocSysLogin(result.docSysUrl, username, password)) {
                result.docSysUsername = username;
                result.docSysPassword = password;
                result.docSysLoginSuccess = true;
                log.info("✅ DocSystem 登录成功: {}", username);
                return;
            }
        }

        result.warnings.add("DocSystem 登录失败，请手动配置管理员凭据");
    }

    /**
     * 加载 jdbc.properties
     */
    private Properties loadJdbcProperties() {
        for (String path : JDBC_PROPERTIES_PATHS) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    Properties props = new Properties();
                    FileInputStream fis = new FileInputStream(file);
                    props.load(fis);
                    fis.close();
                    log.info("✅ 找到 jdbc.properties: {}", path);
                    return props;
                } catch (IOException e) {
                    log.debug("读取 {} 失败: {}", path, e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 测试数据库连接
     */
    private boolean testConnection(String url, Properties props) {
        try {
            String username = props != null ? props.getProperty("jdbc.username", "root") : "root";
            String password = props != null ? props.getProperty("jdbc.password", "") : "";

            Connection conn = DriverManager.getConnection(url, username, password);
            conn.close();
            return true;
        } catch (SQLException e) {
            log.debug("连接失败 {}: {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * 测试 HTTP 端点
     */
    private boolean testHttpEndpoint(String url) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            conn.getOutputStream().write("test=test".getBytes());

            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode >= 200 && responseCode < 500;
        } catch (Exception e) {
            log.debug("HTTP 请求失败 {}: {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * 测试 DocSystem 登录
     */
    private boolean testDocSysLogin(String baseUrl, String username, String password) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(baseUrl + "/User/login.do").openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            // Base64 encode password
            String encodedPwd = java.util.Base64.getEncoder().encodeToString(password.getBytes());
            String postData = "userName=" + username + "&pwd=" + encodedPwd + "&rememberMe=0";
            conn.getOutputStream().write(postData.getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();
                conn.disconnect();
                return response.toString().contains("\"status\":\"ok\"");
            }
            conn.disconnect();
            return false;
        } catch (Exception e) {
            log.debug("登录测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 JDBC URL
     */
    private void parseJdbcUrl(String url, DiscoveryResult result) {
        try {
            // jdbc:mysql://localhost:3307/docsystem
            String cleanUrl = url.substring(5); // remove "jdbc:"
            String[] parts = cleanUrl.split("/");
            String[] hostPort = parts[0].split(":");
            result.mysqlHost = hostPort[0].substring(2); // remove "//"
            if (hostPort.length > 1) {
                result.mysqlPort = Integer.parseInt(hostPort[1]);
            }
            if (parts.length > 1) {
                result.mysqlDatabase = parts[1].split("\\?")[0];
            }
        } catch (Exception e) {
            log.debug("解析 JDBC URL 失败: {}", e.getMessage());
        }
    }

    /**
     * 生成配置建议
     */
    private void generateSuggestions(DiscoveryResult result) {
        if (result.isFullyConfigured()) {
            result.suggestions.add("✅ 环境配置完整，可以正常启动");
            result.suggestions.add("MySQL: " + result.mysqlHost + ":" + result.mysqlPort);
            result.suggestions.add("DocSystem: " + result.docSysUrl);
            result.suggestions.add("DocSystem 登录: " + result.docSysUsername + "/" + result.docSysPassword);
            return;
        }

        // MySQL 未发现
        if (!result.mysqlFound) {
            result.suggestions.add("❌ MySQL 服务未自动发现");
            result.suggestions.add("");
            result.suggestions.add("请执行以下操作之一：");
            result.suggestions.add("1. 启动 MySQL/MariaDB 服务");
            result.suggestions.add("   - 常见端口: 3306, 3307");
            result.suggestions.add("   - 确保 root 用户无密码或使用空密码");
            result.suggestions.add("");
            result.suggestions.add("2. 配置环境变量:");
            result.suggestions.add("   export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/docsystem");
            result.suggestions.add("   export SPRING_DATASOURCE_USERNAME=root");
            result.suggestions.add("   export SPRING_DATASOURCE_PASSWORD=");
        }

        // DocSystem 未发现
        if (!result.docSysFound) {
            if (!result.suggestions.isEmpty()) {
                result.suggestions.add("");
            }
            result.suggestions.add("❌ DocSystem 服务未自动发现");
            result.suggestions.add("");
            result.suggestions.add("请执行以下操作之一：");
            result.suggestions.add("1. 启动 DocSystem (Tomcat)");
            result.suggestions.add("   - 常见端口: 8100, 8200");
            result.suggestions.add("");
            result.suggestions.add("2. 配置环境变量:");
            result.suggestions.add("   export DOCSYS_URL=http://localhost:8100/DocSystem");
        }

        // 登录失败
        if (result.docSysFound && !result.docSysLoginSuccess) {
            if (!result.suggestions.isEmpty()) {
                result.suggestions.add("");
            }
            result.suggestions.add("❌ DocSystem 登录失败");
            result.suggestions.add("");
            result.suggestions.add("请配置 DocSystem 管理员凭据:");
            result.suggestions.add("   export DOCSYS_ADMIN_USERNAME=admin");
            result.suggestions.add("   export DOCSYS_ADMIN_PASSWORD=your_password");
        }
    }

    /**
     * 获取用户友好的配置指南
     */
    public String getConfigurationGuide(DiscoveryResult result) {
        StringBuilder guide = new StringBuilder();
        guide.append("\n");
        guide.append("╔════════════════════════════════════════════════════════════════╗\n");
        guide.append("║          DocSysAgent 环境探索结果                                ║\n");
        guide.append("╠════════════════════════════════════════════════════════════════╣\n");
        guide.append(result.getSummary());
        guide.append("╠════════════════════════════════════════════════════════════════╣\n");

        if (result.isFullyConfigured()) {
            guide.append("║ ✅ 环境配置完整！系统可以正常启动                            ║\n");
        } else {
            guide.append("║ ⚠️  环境配置不完整，请按以下说明操作：                         ║\n");
            guide.append("╠════════════════════════════════════════════════════════════════╣\n");

            for (String suggestion : result.suggestions) {
                if (suggestion.isEmpty()) {
                    guide.append("║                                                            ║\n");
                } else {
                    guide.append("║ ").append(String.format("%-58s", suggestion)).append(" ║\n");
                }
            }

            guide.append("╠════════════════════════════════════════════════════════════════╣\n");
            guide.append("║ 快速启动命令:                                               ║\n");
            guide.append("║                                                            ║\n");
            guide.append("║   # 启动 MySQL (3307)                                       ║\n");
            guide.append("║   cd F:/gy/docsys && mysql/bin/mysqld.exe ...               ║\n");
            guide.append("║                                                            ║\n");
            guide.append("║   # 启动 Tomcat (8100)                                     ║\n");
            guide.append("║   export JAVA_HOME=F:/gy/jdk8/jdk8u432-b06                ║\n");
            guide.append("║   cd F:/gy/docsys/tomcat && bin/startup.bat                 ║\n");
            guide.append("║                                                            ║\n");
            guide.append("║   # 验证服务                                               ║\n");
            guide.append("║   curl http://localhost:8100/DocSystem                      ║\n");
            guide.append("║   curl http://localhost:8100/agent/health                   ║\n");
        }

        guide.append("╚════════════════════════════════════════════════════════════════╝\n");
        return guide.toString();
    }

    /**
     * 将探索结果转换为 Properties 供其他组件使用
     */
    public Properties toProperties(DiscoveryResult result) {
        Properties props = new Properties();

        if (result.mysqlFound) {
            props.setProperty("spring.datasource.url",
                "jdbc:mysql://" + result.mysqlHost + ":" + result.mysqlPort + "/" + result.mysqlDatabase);
            props.setProperty("spring.datasource.username", result.mysqlUsername);
            props.setProperty("spring.datasource.password", result.mysqlPassword);
        }

        if (result.docSysFound) {
            props.setProperty("docsys.url", result.docSysUrl);
            if (result.docSysLoginSuccess) {
                props.setProperty("docsys.admin.username", result.docSysUsername);
                props.setProperty("docsys.admin.password", result.docSysPassword);
            }
        }

        return props;
    }
}
