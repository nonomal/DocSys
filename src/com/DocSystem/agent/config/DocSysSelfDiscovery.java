package com.DocSystem.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * DocSys self-discovery for DataSource configuration (Path A).
 *
 * The agent WAR does NOT carry its own database credentials. At startup it
 * reads the {@code jdbc.properties} file deployed alongside DocSystem in the
 * same Tomcat, and uses those values verbatim. This is the single source of
 * truth (SSOT) for the database connection — no copy in agent's application.yml,
 * no copy in env vars, no copy in agent's @Bean defaults.
 *
 * Resolution priority (first non-empty wins):
 * <ol>
 *   <li>JVM system property {@code -Dspring.datasource.url=...}</li>
 *   <li>Environment variable {@code SPRING_DATASOURCE_URL}</li>
 *   <li>{@code jdbc.properties} auto-discovered on the filesystem (DocSystem war)</li>
 *   <li>Hardcoded fallback {@code jdbc:mysql://localhost:3307/docsystem1}</li>
 * </ol>
 *
 * Why a file (not an HTTP endpoint): at startup time the agent runs in the
 * same JVM as DocSystem (same Tomcat). An HTTP round-trip would be wasted
 * latency and a deployment-order coupling. The file is already on disk by
 * the time the agent boots.
 */
public final class DocSysSelfDiscovery {

    private static final Logger log = LoggerFactory.getLogger(DocSysSelfDiscovery.class);

    /** Resolved JDBC settings plus provenance information. */
    public static final class JdbcSettings {
        public String url;
        public String username;
        public String password;
        public String driverClassName;
        /** Where the values came from: a file path, "&lt;jvm/env&gt;", or "&lt;hardcoded-fallback&gt;". */
        public String sourcePath;

        @Override
        public String toString() {
            return "JdbcSettings{url=" + url + ", username=" + username
                + ", driverClassName=" + driverClassName
                + ", sourcePath=" + sourcePath + "}";
        }
    }

    private DocSysSelfDiscovery() {
        // utility class
    }

    /**
     * Build a Spring {@link DataSource} from the resolved settings.
     * Uses Spring's {@link DriverManagerDataSource} for parity with the
     * pre-existing @Bean (no connection pool at this layer; Flyway and
     * MyBatis will pool via their own mechanisms).
     */
    public static DataSource buildDataSource() {
        JdbcSettings s = resolve();
        log.info("DocSysSelfDiscovery resolved: {}", s);
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(s.url);
        ds.setUsername(s.username);
        ds.setPassword(s.password == null ? "" : s.password);
        ds.setDriverClassName(s.driverClassName);
        return ds;
    }

    /**
     * Resolve JDBC settings from the priority chain. Returns a non-null
     * JdbcSettings in all cases — falls back to hardcoded localhost if
     * DocSystem is not deployed alongside (typical for unit tests).
     *
     * Port auto-detection: For any discovered URL (from JVM/env, file, or fallback),
     * if the port is not explicitly specified or connection fails, automatically
     * tries common MySQL ports (3306, 3307) to find a working connection.
     */
    public static JdbcSettings resolve() {
        JdbcSettings fromJvm = readFromJvmOrEnv();
        if (fromJvm.url != null && !fromJvm.url.isEmpty()) {
            fromJvm.sourcePath = "<jvm/env>";
            return detectWorkingPort(fromJvm);
        }
        JdbcSettings fromFile = loadFromDocSystemWebapp();
        if (fromFile != null) {
            log.info("Discovered DocSystem jdbc.properties at {}", fromFile.sourcePath);
            return detectWorkingPort(fromFile);
        }
        log.warn("DocSystem jdbc.properties not found; using hardcoded fallback with port auto-detection");
        JdbcSettings fallback = new JdbcSettings();
        fallback.url = "jdbc:mysql://localhost/docsystem1?useSSL=false"
            + "&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
        fallback.username = "root";
        fallback.password = "";
        fallback.driverClassName = "com.mysql.jdbc.Driver";
        fallback.sourcePath = "<hardcoded-fallback>";
        return detectWorkingPort(fallback);
    }

    /**
     * Read URL/username/password/driver from JVM system properties or
     * environment variables. Returns a JdbcSettings with all fields set
     * to defaults; the caller checks {@code url} for null/empty.
     */
    private static JdbcSettings readFromJvmOrEnv() {
        JdbcSettings s = new JdbcSettings();
        s.url = sysOrEnv("spring.datasource.url", null);
        s.username = sysOrEnv("spring.datasource.username", "root");
        s.password = sysOrEnv("spring.datasource.password", "");
        s.driverClassName = sysOrEnv("spring.datasource.driver-class-name",
            "com.mysql.jdbc.Driver");
        s.sourcePath = "<jvm/env>";
        return s;
    }

    /**
     * Search the filesystem for DocSystem's {@code jdbc.properties}. Returns
     * null if no candidate exists (caller falls back to hardcoded).
     */
    private static JdbcSettings loadFromDocSystemWebapp() {
        String catalinaHome = System.getProperty("catalina.home");
        if (catalinaHome == null) {
            catalinaHome = System.getenv("CATALINA_HOME");
        }
        if (catalinaHome == null || catalinaHome.isEmpty()) {
            log.debug("catalina.home not set; cannot discover DocSystem jdbc.properties");
            return null;
        }

        // Candidate paths in priority order. DocSystem war usually deploys
        // as webapps/DocSystem/ (capital S), but the unpacked dir name may
        // be lowercased on some filesystems. ROOT/DocSystem covers the
        // case where DocSystem is mounted under the ROOT webapp.
        List<String> candidates = Arrays.asList(
            catalinaHome + "/webapps/DocSystem/WEB-INF/classes/jdbc.properties",
            catalinaHome + "/webapps/ROOT/DocSystem/WEB-INF/classes/jdbc.properties",
            catalinaHome + "/webapps/docsystem/WEB-INF/classes/jdbc.properties"
        );

        for (String path : candidates) {
            Path p = Paths.get(path);
            if (!Files.exists(p)) {
                continue;
            }
            Properties props = new Properties();
            try (InputStream in = new FileInputStream(p.toFile())) {
                props.load(in);
            } catch (IOException e) {
                log.warn("Found jdbc.properties at {} but failed to read: {}", path, e.getMessage());
                continue;
            }
            JdbcSettings s = new JdbcSettings();
            s.url = trimToNull(props.getProperty("db.url"));
            s.username = props.getProperty("db.username", "root");
            s.password = props.getProperty("db.password", "");
            s.driverClassName = props.getProperty("db.driver", "com.mysql.jdbc.Driver");
            s.sourcePath = path;
            if (s.url != null) {
                return s;
            }
            log.warn("jdbc.properties at {} has empty db.url; skipping", path);
        }
        return null;
    }

    private static String sysOrEnv(String key, String fallback) {
        String v = System.getProperty(key);
        if (v == null || v.isEmpty()) {
            // Convert "spring.datasource.url" → "SPRING_DATASOURCE_URL"
            v = System.getenv(key.replace('.', '_').toUpperCase());
        }
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Auto-detect working MySQL port. If the URL has an explicit port, test it.
     * If connection fails or no port specified, try common ports (3306, 3307).
     * Returns a JdbcSettings with a working URL.
     */
    private static JdbcSettings detectWorkingPort(JdbcSettings settings) {
        if (settings == null || settings.url == null) {
            return settings;
        }

        // Extract host, database, and query params from URL
        String baseUrl = extractBaseUrl(settings.url);
        String host = extractHost(settings.url);
        String database = extractDatabase(settings.url);
        String queryParams = extractQueryParams(settings.url);

        // First, try the URL as-is (if it has an explicit port)
        Integer explicitPort = extractPort(settings.url);
        if (explicitPort != null) {
            if (testConnection(host, explicitPort, settings.username, settings.password, settings.driverClassName)) {
                log.info("Connected to MySQL at {}:{} using explicit port", host, explicitPort);
                return settings;
            }
            log.warn("Failed to connect using explicit port {}, will try auto-detection", explicitPort);
        }

        // Auto-detect: try common MySQL ports
        int[] commonPorts = {3306, 3307};
        for (int port : commonPorts) {
            if (explicitPort != null && port == explicitPort) {
                continue; // Already tried this port
            }
            if (testConnection(host, port, settings.username, settings.password, settings.driverClassName)) {
                log.info("Auto-detected working MySQL port: {} for host {}", port, host);
                // Build new URL with detected port
                settings.url = buildUrl(baseUrl, host, port, database, queryParams);
                return settings;
            }
        }

        // If all attempts failed, log warning but return original settings
        log.warn("Could not establish connection to MySQL at {} on any common port (3306, 3307). "
               + "Using original URL and hoping for the best.", host);
        return settings;
    }

    /**
     * Test if a MySQL connection works on the specified port.
     */
    private static boolean testConnection(String host, int port, String username, String password, String driverClassName) {
        Connection conn = null;
        try {
            // Load driver if needed
            Class.forName(driverClassName);

            // Test connection with minimal timeout
            String testUrl = "jdbc:mysql://" + host + ":" + port
                + "?useSSL=false&connectTimeout=3000&socketTimeout=3000";
            conn = DriverManager.getConnection(testUrl, username, password);
            log.debug("Successfully tested connection to {}:{}", host, port);
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            log.debug("Connection test failed for {}:{}: {}", host, port, e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore close errors
                }
            }
        }
    }

    private static String extractBaseUrl(String url) {
        // jdbc:mysql://
        int protocolEnd = url.indexOf("://");
        return protocolEnd > 0 ? url.substring(0, protocolEnd + 3) : "jdbc:mysql://";
    }

    private static String extractHost(String url) {
        // Extract host from jdbc:mysql://host:port/database?params
        int start = url.indexOf("://") + 3;
        int authorityEnd = url.indexOf("/", start);
        if (authorityEnd < 0) authorityEnd = url.indexOf("?");
        if (authorityEnd < 0) authorityEnd = url.length();

        String authority = url.substring(start, authorityEnd);
        // Remove port if present
        int colonPos = authority.indexOf(':');
        if (colonPos > 0) {
            return authority.substring(0, colonPos);
        }
        return authority;
    }

    private static Integer extractPort(String url) {
        // Extract port from URL, or null if not specified
        int start = url.indexOf("://") + 3;
        int authorityEnd = url.indexOf("/", start);
        if (authorityEnd < 0) authorityEnd = url.indexOf("?");
        if (authorityEnd < 0) authorityEnd = url.length();

        String authority = url.substring(start, authorityEnd);
        int colonPos = authority.indexOf(':');
        if (colonPos > 0) {
            try {
                return Integer.parseInt(authority.substring(colonPos + 1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String extractDatabase(String url) {
        // Extract database name from /database?params
        int start = url.indexOf("/", url.indexOf("://") + 3);
        if (start < 0) return "";

        int end = url.indexOf("?", start);
        if (end < 0) end = url.length();

        return url.substring(start + 1, end);
    }

    private static String extractQueryParams(String url) {
        // Extract everything after ? (including the ?)
        int queryStart = url.indexOf("?");
        return queryStart >= 0 ? url.substring(queryStart) : "?useSSL=false";
    }

    private static String buildUrl(String baseUrl, String host, int port, String database, String queryParams) {
        return baseUrl + host + ":" + port + "/" + database + queryParams;
    }
}
