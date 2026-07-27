package com.docsys.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Automatic database table initialization on startup.
 *
 * Checks if required DocSysAgent tables exist, and creates them if missing.
 * Uses CREATE TABLE IF NOT EXISTS for safety - can be run multiple times.
 *
 * Compatible with MariaDB 10.1+ (uses LONGTEXT instead of JSON type).
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private DataSource dataSource;

    /** All required DocSysAgent tables */
    private static final String[] REQUIRED_TABLES = {
        "agent_sessions",
        "agent_tasks",
        "audit_logs",
        "skill_metadata",
        "user_permissions",
        "user_behavior_tags",
        "user_experiences",
        "shared_knowledge",
        "collaborative_recommendations",
        "skill_ratings",
        "similar_users_cache"
    };

    /** CREATE TABLE statements (MariaDB 10.1 compatible with LONGTEXT) */
    private static final String[] CREATE_TABLE_STATEMENTS = {
        // agent_sessions
        "CREATE TABLE IF NOT EXISTS agent_sessions ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "session_id VARCHAR(64) NOT NULL COMMENT 'Agent session ID (UUID)',"
            + "username VARCHAR(128) COMMENT 'Authenticated username',"
            + "jsessionid VARCHAR(256) COMMENT 'DocSystem JSESSIONID cookie value',"
            + "tenant_id VARCHAR(64) COMMENT 'Tenant identifier',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
            + "expires_at TIMESTAMP NULL COMMENT 'Session expiry (NULL = no expiry)',"
            + "metadata LONGTEXT COMMENT 'Additional session metadata (JSON-encoded)',"
            + "UNIQUE KEY uk_session_id (session_id),"
            + "INDEX idx_username (username),"
            + "INDEX idx_last_active (last_active),"
            + "INDEX idx_expires (expires_at)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent session table (MariaDB 10.1 compatible)'",

        // agent_tasks
        "CREATE TABLE IF NOT EXISTS agent_tasks ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "task_id VARCHAR(64) NOT NULL COMMENT 'Unique task identifier (UUID)',"
            + "session_id VARCHAR(64) COMMENT 'Associated agent session ID',"
            + "task_type VARCHAR(64) NOT NULL COMMENT 'Task type (e.g., list_repos, delete_doc)',"
            + "task_params LONGTEXT COMMENT 'Task parameters as JSON',"
            + "status ENUM('PENDING','RUNNING','COMPLETED','FAILED','RETRY_PENDING') NOT NULL DEFAULT 'PENDING',"
            + "retry_count INT NOT NULL DEFAULT 0 COMMENT 'Number of retry attempts',"
            + "max_retries INT NOT NULL DEFAULT 3 COMMENT 'Maximum retry attempts',"
            + "result TEXT COMMENT 'Task execution result',"
            + "error_message TEXT COMMENT 'Last error message',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "started_at TIMESTAMP NULL COMMENT 'When task started running',"
            + "completed_at TIMESTAMP NULL COMMENT 'When task completed/failed',"
            + "timeout_at TIMESTAMP NULL COMMENT 'When task should be considered timed out',"
            + "metadata LONGTEXT COMMENT 'Additional task metadata (traceId, userId, etc.) as JSON',"
            + "UNIQUE KEY uk_task_id (task_id),"
            + "INDEX idx_status (status),"
            + "INDEX idx_session (session_id),"
            + "INDEX idx_timeout (timeout_at),"
            + "INDEX idx_created (created_at),"
            + "INDEX idx_status_retry (status, retry_count)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent task queue table'",

        // audit_logs
        "CREATE TABLE IF NOT EXISTS audit_logs ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "user_id VARCHAR(128) NOT NULL COMMENT 'Sanitized user identifier',"
            + "session_id VARCHAR(64) COMMENT 'Agent session ID',"
            + "operation VARCHAR(64) NOT NULL COMMENT 'Operation type (e.g., delete_repos, upload_doc)',"
            + "operation_params TEXT COMMENT 'Sanitized operation parameters (JSON, no PII)',"
            + "status ENUM('PENDING','APPROVED','REJECTED','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',"
            + "confirm_token VARCHAR(64) COMMENT 'Token for SSE confirm/reject flow (UX-01)',"
            + "client_ip VARCHAR(45) COMMENT 'Client IP address (IPv4 or IPv6)',"
            + "trace_id VARCHAR(64) COMMENT 'OpenTelemetry trace ID for correlation',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "completed_at TIMESTAMP NULL COMMENT 'When operation was approved/rejected/completed',"
            + "result_message TEXT COMMENT 'Operation result or error message',"
            + "INDEX idx_user_id (user_id),"
            + "INDEX idx_session (session_id),"
            + "INDEX idx_operation (operation),"
            + "INDEX idx_status (status),"
            + "INDEX idx_created (created_at),"
            + "INDEX idx_trace (trace_id)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Operation audit log'",

        // skill_metadata
        "CREATE TABLE IF NOT EXISTS skill_metadata ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "skill_id VARCHAR(64) NOT NULL COMMENT '技能ID',"
            + "creator_id VARCHAR(64) NOT NULL COMMENT '上传者用户ID',"
            + "creator_name VARCHAR(128) COMMENT '上传者名称',"
            + "visibility ENUM('PRIVATE', 'TENANT', 'PUBLIC') DEFAULT 'PRIVATE' COMMENT '可见范围',"
            + "allowed_user_ids LONGTEXT COMMENT '允许使用的用户ID列表(JSON数组)',"
            + "is_admin_skill BOOLEAN DEFAULT FALSE COMMENT '是否管理员上传的技能',"
            + "tenant_id VARCHAR(64) COMMENT '租户ID',"
            + "name VARCHAR(256) COMMENT '技能名称',"
            + "category VARCHAR(64) COMMENT '技能分类',"
            + "version VARCHAR(32) COMMENT '版本号',"
            + "file_path VARCHAR(512) COMMENT '技能文件路径',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
            + "UNIQUE KEY uk_skill_id (skill_id),"
            + "INDEX idx_creator (creator_id),"
            + "INDEX idx_visibility (visibility),"
            + "INDEX idx_tenant (tenant_id),"
            + "INDEX idx_created (created_at DESC)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能元数据表-支持用户级隔离和可见范围控制'",

        // user_permissions
        "CREATE TABLE IF NOT EXISTS user_permissions ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "user_id VARCHAR(64) NOT NULL,"
            + "tenant_id VARCHAR(64),"
            + "role_id VARCHAR(64),"
            + "role_name VARCHAR(128),"
            + "department_id VARCHAR(64),"
            + "permission_level INT DEFAULT 0 COMMENT '0=普通用户, 1=管理员, 2=超级管理员',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
            + "UNIQUE KEY uk_user_role (user_id, role_id),"
            + "INDEX idx_tenant (tenant_id),"
            + "INDEX idx_permission (permission_level),"
            + "INDEX idx_user (user_id)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权限表'",

        // user_behavior_tags
        "CREATE TABLE IF NOT EXISTS user_behavior_tags ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "user_id VARCHAR(64) NOT NULL,"
            + "tenant_id VARCHAR(64),"
            + "action_tag VARCHAR(64) NOT NULL COMMENT '行为标签',"
            + "action_count INT DEFAULT 1 COMMENT '行为次数',"
            + "last_action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
            + "UNIQUE KEY uk_user_tag (user_id, action_tag),"
            + "INDEX idx_tag_count (action_tag, action_count DESC),"
            + "INDEX idx_tenant_tag (tenant_id, action_tag),"
            + "INDEX idx_user (user_id)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为标签表'",

        // user_experiences
        "CREATE TABLE IF NOT EXISTS user_experiences ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "user_id VARCHAR(64) NOT NULL,"
            + "tenant_id VARCHAR(64),"
            + "query TEXT NOT NULL COMMENT '用户问题',"
            + "intent VARCHAR(64) NOT NULL COMMENT '识别的意图',"
            + "actions LONGTEXT COMMENT '执行的动作(JSON)',"
            + "success BOOLEAN DEFAULT TRUE COMMENT '是否成功',"
            + "duration_ms INT COMMENT '执行耗时',"
            + "error_message TEXT COMMENT '错误信息',"
            + "feedback_score INT COMMENT '用户反馈: 1=差, 2=一般, 3=好, 4=很好, 5=完美',"
            + "feedback_text TEXT COMMENT '用户反馈文字',"
            + "share_level ENUM('PRIVATE', 'TENANT', 'PUBLIC', 'INVITED') DEFAULT 'PRIVATE',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "indexed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "INDEX idx_user_intent (user_id, intent),"
            + "INDEX idx_intent_success (intent, success),"
            + "INDEX idx_tenant (tenant_id),"
            + "INDEX idx_created (created_at),"
            + "INDEX idx_share_level (share_level)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户经验表'",

        // shared_knowledge
        "CREATE TABLE IF NOT EXISTS shared_knowledge ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "intent VARCHAR(64) NOT NULL COMMENT '意图类型',"
            + "query_pattern TEXT COMMENT '匹配模式',"
            + "solution LONGTEXT NOT NULL COMMENT '解决方案(JSON)',"
            + "success_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '成功率',"
            + "usage_count INT DEFAULT 0 COMMENT '使用次数',"
            + "contributor_id VARCHAR(64) COMMENT '贡献者',"
            + "contributor_name VARCHAR(128),"
            + "tenant_id VARCHAR(64),"
            + "share_level ENUM('TENANT', 'PUBLIC') DEFAULT 'PUBLIC',"
            + "status ENUM('DRAFT', 'ACTIVE', 'DEPRECATED') DEFAULT 'DRAFT',"
            + "approved_at TIMESTAMP NULL,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
            + "INDEX idx_intent (intent),"
            + "INDEX idx_share_level (share_level, tenant_id),"
            + "INDEX idx_success_rate (success_rate DESC),"
            + "INDEX idx_status (status)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公共知识库'",

        // collaborative_recommendations
        "CREATE TABLE IF NOT EXISTS collaborative_recommendations ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "tenant_id VARCHAR(64),"
            + "target_user_id VARCHAR(64) NOT NULL COMMENT '目标用户',"
            + "recommended_intent VARCHAR(128) NOT NULL COMMENT '推荐意图',"
            + "recommended_actions LONGTEXT COMMENT '推荐动作(JSON)',"
            + "confidence_score DECIMAL(5,4) DEFAULT 0.0000 COMMENT '置信度 0.0000-1.0000',"
            + "source_user_ids LONGTEXT COMMENT '参考的相似用户ID列表(JSON)',"
            + "source_count INT DEFAULT 0 COMMENT '相似用户数量',"
            + "reason TEXT COMMENT '推荐理由',"
            + "is_clicked BOOLEAN DEFAULT FALSE COMMENT '是否被点击',"
            + "is_useful BOOLEAN DEFAULT NULL COMMENT '是否有用',"
            + "expires_at TIMESTAMP COMMENT '过期时间',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
            + "INDEX idx_target (target_user_id, expires_at),"
            + "INDEX idx_confidence (confidence_score DESC),"
            + "INDEX idx_tenant (tenant_id),"
            + "INDEX idx_clicked (is_clicked)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协同推荐缓存'",

        // skill_ratings
        "CREATE TABLE IF NOT EXISTS skill_ratings ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "skill_id VARCHAR(64) NOT NULL COMMENT '技能ID',"
            + "user_id VARCHAR(64) NOT NULL COMMENT '评分用户',"
            + "tenant_id VARCHAR(64),"
            + "rating INT NOT NULL COMMENT '评分 1-5',"
            + "comment TEXT COMMENT '评价文字',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "UNIQUE KEY uk_user_skill (user_id, skill_id),"
            + "INDEX idx_skill (skill_id),"
            + "INDEX idx_user (user_id)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能评分表'",

        // similar_users_cache
        "CREATE TABLE IF NOT EXISTS similar_users_cache ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
            + "user_id VARCHAR(64) NOT NULL,"
            + "tenant_id VARCHAR(64),"
            + "similar_user_id VARCHAR(64) NOT NULL,"
            + "similarity_score DECIMAL(5,4) NOT NULL COMMENT '相似度 0.0000-1.0000',"
            + "permission_similarity DECIMAL(5,4) DEFAULT 0.0000 COMMENT '权限相似度',"
            + "behavior_similarity DECIMAL(5,4) DEFAULT 0.0000 COMMENT '行为相似度',"
            + "computed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "expires_at TIMESTAMP COMMENT '过期时间',"
            + "UNIQUE KEY uk_user_similar (user_id, similar_user_id),"
            + "INDEX idx_user_similarity (user_id, similarity_score DESC),"
            + "INDEX idx_computed (computed_at)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相似用户缓存'"
    };

    @PostConstruct
    public void init() {
        log.info("Starting database table initialization check...");

        try (Connection conn = dataSource.getConnection()) {
            String databaseName = getDatabaseName(conn);
            log.info("Connected to database: {}", databaseName);

            List<String> missingTables = checkMissingTables(conn, databaseName);

            if (missingTables.isEmpty()) {
                log.info("All required DocSysAgent tables exist. Initialization complete.");
                return;
            }

            log.warn("Found {} missing tables: {}", missingTables.size(), missingTables);
            log.info("Creating missing tables...");

            int created = createMissingTables(conn, missingTables);

            log.info("Successfully created {} tables. Database initialization complete.", created);
            log.info("DocSysAgent tables initialized: agent_sessions, agent_tasks, audit_logs, "
                    + "skill_metadata, user_permissions, user_behavior_tags, user_experiences, "
                    + "shared_knowledge, collaborative_recommendations, skill_ratings, similar_users_cache");

        } catch (SQLException e) {
            log.error("Failed to initialize database tables: {}", e.getMessage(), e);
            // Don't throw - allow app to start even if table creation fails
            // Tables can be created manually if needed
        }
    }

    /**
     * Get the current database name from connection.
     */
    private String getDatabaseName(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT DATABASE()")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "unknown";
    }

    /**
     * Check which required tables are missing.
     */
    private List<String> checkMissingTables(Connection conn, String databaseName) throws SQLException {
        List<String> missing = new ArrayList<>();

        // Query information_schema to check existing tables
        String sql = "SELECT table_name FROM information_schema.tables "
                   + "WHERE table_schema = ? AND table_name IN (";

        // Build placeholder list for IN clause
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < REQUIRED_TABLES.length; i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        sql += placeholders.toString() + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            for (int i = 0; i < REQUIRED_TABLES.length; i++) {
                ps.setString(i + 2, REQUIRED_TABLES[i]);
            }

            ResultSet rs = ps.executeQuery();
            List<String> existing = new ArrayList<>();
            while (rs.next()) {
                existing.add(rs.getString("table_name"));
            }

            // Find missing tables
            for (String table : REQUIRED_TABLES) {
                if (!existing.contains(table)) {
                    missing.add(table);
                }
            }
        }

        return missing;
    }

    /**
     * Create missing tables using CREATE TABLE IF NOT EXISTS.
     */
    private int createMissingTables(Connection conn, List<String> missingTables) throws SQLException {
        int created = 0;

        for (String sql : CREATE_TABLE_STATEMENTS) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
                created++;
                log.debug("Executed CREATE TABLE for: {}", extractTableName(sql));
            } catch (SQLException e) {
                log.warn("Failed to create table (may already exist): {}", e.getMessage());
                // Continue with other tables
            }
        }

        return created;
    }

    /**
     * Extract table name from CREATE TABLE statement for logging.
     */
    private String extractTableName(String sql) {
        // Pattern: CREATE TABLE IF NOT EXISTS table_name ...
        int start = sql.toUpperCase().indexOf("CREATE TABLE IF NOT EXISTS ");
        if (start < 0) start = sql.toUpperCase().indexOf("CREATE TABLE ");
        if (start < 0) return "unknown";

        start = sql.indexOf(" ", start + 6) + 1; // Skip "CREATE TABLE"
        int end = sql.indexOf(" ", start);
        if (end < 0) end = sql.indexOf("(", start);
        if (end < 0) end = sql.length();

        String table = sql.substring(start, end).trim();
        // Remove backticks if present
        return table.replace("`", "");
    }
}
