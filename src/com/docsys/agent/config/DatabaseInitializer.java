package com.docsys.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    /**
     * SQLite-dialect CREATE TABLE statements (parallel to CREATE_TABLE_STATEMENTS).
     *
     * Differences from the MySQL versions:
     * - id INTEGER PRIMARY KEY AUTOINCREMENT (SQLite rowid, not BIGINT/AUTO_INCREMENT)
     * - No COMMENT clauses (column or table level), no ENGINE/CHARSET table options
     * - ENUM(...) columns replaced with VARCHAR(32)
     * - LONGTEXT replaced with TEXT
     * - ON UPDATE CURRENT_TIMESTAMP removed (unsupported by SQLite)
     * - BOOLEAN DEFAULT FALSE/TRUE mapped to DEFAULT 0/1
     * - UNIQUE KEY ... (...) rewritten as a table-level UNIQUE (...) constraint
     * - Non-unique INDEX definitions moved OUT into CREATE_INDEX_STATEMENTS_SQLITE
     */
    private static final String[] CREATE_TABLE_STATEMENTS_SQLITE = {
        // agent_sessions
        "CREATE TABLE IF NOT EXISTS agent_sessions ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "session_id VARCHAR(64) NOT NULL,"
            + "username VARCHAR(128),"
            + "jsessionid VARCHAR(256),"
            + "tenant_id VARCHAR(64),"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "expires_at TIMESTAMP NULL,"
            + "metadata TEXT,"
            + "UNIQUE (session_id)"
            + ")",

        // agent_tasks
        "CREATE TABLE IF NOT EXISTS agent_tasks ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "task_id VARCHAR(64) NOT NULL,"
            + "session_id VARCHAR(64),"
            + "task_type VARCHAR(64) NOT NULL,"
            + "task_params TEXT,"
            + "status VARCHAR(32) NOT NULL DEFAULT 'PENDING',"
            + "retry_count INT NOT NULL DEFAULT 0,"
            + "max_retries INT NOT NULL DEFAULT 3,"
            + "result TEXT,"
            + "error_message TEXT,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "started_at TIMESTAMP NULL,"
            + "completed_at TIMESTAMP NULL,"
            + "timeout_at TIMESTAMP NULL,"
            + "metadata TEXT,"
            + "UNIQUE (task_id)"
            + ")",

        // audit_logs
        "CREATE TABLE IF NOT EXISTS audit_logs ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "user_id VARCHAR(128) NOT NULL,"
            + "session_id VARCHAR(64),"
            + "operation VARCHAR(64) NOT NULL,"
            + "operation_params TEXT,"
            + "status VARCHAR(32) NOT NULL DEFAULT 'PENDING',"
            + "confirm_token VARCHAR(64),"
            + "client_ip VARCHAR(45),"
            + "trace_id VARCHAR(64),"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "completed_at TIMESTAMP NULL,"
            + "result_message TEXT"
            + ")",

        // skill_metadata
        "CREATE TABLE IF NOT EXISTS skill_metadata ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "skill_id VARCHAR(64) NOT NULL,"
            + "creator_id VARCHAR(64) NOT NULL,"
            + "creator_name VARCHAR(128),"
            + "visibility VARCHAR(32) DEFAULT 'PRIVATE',"
            + "allowed_user_ids TEXT,"
            + "is_admin_skill BOOLEAN DEFAULT 0,"
            + "tenant_id VARCHAR(64),"
            + "name VARCHAR(256),"
            + "category VARCHAR(64),"
            + "version VARCHAR(32),"
            + "file_path VARCHAR(512),"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "UNIQUE (skill_id)"
            + ")",

        // user_permissions
        "CREATE TABLE IF NOT EXISTS user_permissions ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "user_id VARCHAR(64) NOT NULL,"
            + "tenant_id VARCHAR(64),"
            + "role_id VARCHAR(64),"
            + "role_name VARCHAR(128),"
            + "department_id VARCHAR(64),"
            + "permission_level INT DEFAULT 0,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "UNIQUE (user_id, role_id)"
            + ")",

        // user_behavior_tags
        "CREATE TABLE IF NOT EXISTS user_behavior_tags ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "user_id VARCHAR(64) NOT NULL,"
            + "tenant_id VARCHAR(64),"
            + "action_tag VARCHAR(64) NOT NULL,"
            + "action_count INT DEFAULT 1,"
            + "last_action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "UNIQUE (user_id, action_tag)"
            + ")",

        // user_experiences
        "CREATE TABLE IF NOT EXISTS user_experiences ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "user_id VARCHAR(64) NOT NULL,"
            + "tenant_id VARCHAR(64),"
            + "query TEXT NOT NULL,"
            + "intent VARCHAR(64) NOT NULL,"
            + "actions TEXT,"
            + "success BOOLEAN DEFAULT 1,"
            + "duration_ms INT,"
            + "error_message TEXT,"
            + "feedback_score INT,"
            + "feedback_text TEXT,"
            + "share_level VARCHAR(32) DEFAULT 'PRIVATE',"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "indexed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
            + ")",

        // shared_knowledge
        "CREATE TABLE IF NOT EXISTS shared_knowledge ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "intent VARCHAR(64) NOT NULL,"
            + "query_pattern TEXT,"
            + "solution TEXT NOT NULL,"
            + "success_rate DECIMAL(5,2) DEFAULT 0.00,"
            + "usage_count INT DEFAULT 0,"
            + "contributor_id VARCHAR(64),"
            + "contributor_name VARCHAR(128),"
            + "tenant_id VARCHAR(64),"
            + "share_level VARCHAR(32) DEFAULT 'PUBLIC',"
            + "status VARCHAR(32) DEFAULT 'DRAFT',"
            + "approved_at TIMESTAMP NULL,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
            + ")",

        // collaborative_recommendations
        "CREATE TABLE IF NOT EXISTS collaborative_recommendations ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "tenant_id VARCHAR(64),"
            + "target_user_id VARCHAR(64) NOT NULL,"
            + "recommended_intent VARCHAR(128) NOT NULL,"
            + "recommended_actions TEXT,"
            + "confidence_score DECIMAL(5,4) DEFAULT 0.0000,"
            + "source_user_ids TEXT,"
            + "source_count INT DEFAULT 0,"
            + "reason TEXT,"
            + "is_clicked BOOLEAN DEFAULT 0,"
            + "is_useful BOOLEAN DEFAULT NULL,"
            + "expires_at TIMESTAMP,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
            + ")",

        // skill_ratings
        "CREATE TABLE IF NOT EXISTS skill_ratings ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "skill_id VARCHAR(64) NOT NULL,"
            + "user_id VARCHAR(64) NOT NULL,"
            + "tenant_id VARCHAR(64),"
            + "rating INT NOT NULL,"
            + "comment TEXT,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "UNIQUE (user_id, skill_id)"
            + ")",

        // similar_users_cache
        "CREATE TABLE IF NOT EXISTS similar_users_cache ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "user_id VARCHAR(64) NOT NULL,"
            + "tenant_id VARCHAR(64),"
            + "similar_user_id VARCHAR(64) NOT NULL,"
            + "similarity_score DECIMAL(5,4) NOT NULL,"
            + "permission_similarity DECIMAL(5,4) DEFAULT 0.0000,"
            + "behavior_similarity DECIMAL(5,4) DEFAULT 0.0000,"
            + "computed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "expires_at TIMESTAMP,"
            + "UNIQUE (user_id, similar_user_id)"
            + ")"
    };

    /**
     * SQLite CREATE INDEX statements extracted from the inline MySQL INDEX definitions.
     *
     * SQLite does NOT allow inline non-unique INDEX inside CREATE TABLE, and its index
     * names live in a single DB-global namespace (unlike MySQL where they are per-table).
     * Since several tables reuse names like idx_created / idx_tenant / idx_user / idx_status,
     * every index name here is PREFIXED with its table name to avoid global collisions.
     * Executed only for the SQLite dialect, after the table creates.
     */
    private static final String[] CREATE_INDEX_STATEMENTS_SQLITE = {
        // agent_sessions
        "CREATE INDEX IF NOT EXISTS agent_sessions_idx_username ON agent_sessions (username)",
        "CREATE INDEX IF NOT EXISTS agent_sessions_idx_last_active ON agent_sessions (last_active)",
        "CREATE INDEX IF NOT EXISTS agent_sessions_idx_expires ON agent_sessions (expires_at)",
        // agent_tasks
        "CREATE INDEX IF NOT EXISTS agent_tasks_idx_status ON agent_tasks (status)",
        "CREATE INDEX IF NOT EXISTS agent_tasks_idx_session ON agent_tasks (session_id)",
        "CREATE INDEX IF NOT EXISTS agent_tasks_idx_timeout ON agent_tasks (timeout_at)",
        "CREATE INDEX IF NOT EXISTS agent_tasks_idx_created ON agent_tasks (created_at)",
        "CREATE INDEX IF NOT EXISTS agent_tasks_idx_status_retry ON agent_tasks (status, retry_count)",
        // audit_logs
        "CREATE INDEX IF NOT EXISTS audit_logs_idx_user_id ON audit_logs (user_id)",
        "CREATE INDEX IF NOT EXISTS audit_logs_idx_session ON audit_logs (session_id)",
        "CREATE INDEX IF NOT EXISTS audit_logs_idx_operation ON audit_logs (operation)",
        "CREATE INDEX IF NOT EXISTS audit_logs_idx_status ON audit_logs (status)",
        "CREATE INDEX IF NOT EXISTS audit_logs_idx_created ON audit_logs (created_at)",
        "CREATE INDEX IF NOT EXISTS audit_logs_idx_trace ON audit_logs (trace_id)",
        // skill_metadata
        "CREATE INDEX IF NOT EXISTS skill_metadata_idx_creator ON skill_metadata (creator_id)",
        "CREATE INDEX IF NOT EXISTS skill_metadata_idx_visibility ON skill_metadata (visibility)",
        "CREATE INDEX IF NOT EXISTS skill_metadata_idx_tenant ON skill_metadata (tenant_id)",
        "CREATE INDEX IF NOT EXISTS skill_metadata_idx_created ON skill_metadata (created_at DESC)",
        // user_permissions
        "CREATE INDEX IF NOT EXISTS user_permissions_idx_tenant ON user_permissions (tenant_id)",
        "CREATE INDEX IF NOT EXISTS user_permissions_idx_permission ON user_permissions (permission_level)",
        "CREATE INDEX IF NOT EXISTS user_permissions_idx_user ON user_permissions (user_id)",
        // user_behavior_tags
        "CREATE INDEX IF NOT EXISTS user_behavior_tags_idx_tag_count ON user_behavior_tags (action_tag, action_count DESC)",
        "CREATE INDEX IF NOT EXISTS user_behavior_tags_idx_tenant_tag ON user_behavior_tags (tenant_id, action_tag)",
        "CREATE INDEX IF NOT EXISTS user_behavior_tags_idx_user ON user_behavior_tags (user_id)",
        // user_experiences
        "CREATE INDEX IF NOT EXISTS user_experiences_idx_user_intent ON user_experiences (user_id, intent)",
        "CREATE INDEX IF NOT EXISTS user_experiences_idx_intent_success ON user_experiences (intent, success)",
        "CREATE INDEX IF NOT EXISTS user_experiences_idx_tenant ON user_experiences (tenant_id)",
        "CREATE INDEX IF NOT EXISTS user_experiences_idx_created ON user_experiences (created_at)",
        "CREATE INDEX IF NOT EXISTS user_experiences_idx_share_level ON user_experiences (share_level)",
        // shared_knowledge
        "CREATE INDEX IF NOT EXISTS shared_knowledge_idx_intent ON shared_knowledge (intent)",
        "CREATE INDEX IF NOT EXISTS shared_knowledge_idx_share_level ON shared_knowledge (share_level, tenant_id)",
        "CREATE INDEX IF NOT EXISTS shared_knowledge_idx_success_rate ON shared_knowledge (success_rate DESC)",
        "CREATE INDEX IF NOT EXISTS shared_knowledge_idx_status ON shared_knowledge (status)",
        // collaborative_recommendations
        "CREATE INDEX IF NOT EXISTS collaborative_recommendations_idx_target ON collaborative_recommendations (target_user_id, expires_at)",
        "CREATE INDEX IF NOT EXISTS collaborative_recommendations_idx_confidence ON collaborative_recommendations (confidence_score DESC)",
        "CREATE INDEX IF NOT EXISTS collaborative_recommendations_idx_tenant ON collaborative_recommendations (tenant_id)",
        "CREATE INDEX IF NOT EXISTS collaborative_recommendations_idx_clicked ON collaborative_recommendations (is_clicked)",
        // skill_ratings
        "CREATE INDEX IF NOT EXISTS skill_ratings_idx_skill ON skill_ratings (skill_id)",
        "CREATE INDEX IF NOT EXISTS skill_ratings_idx_user ON skill_ratings (user_id)",
        // similar_users_cache
        "CREATE INDEX IF NOT EXISTS similar_users_cache_idx_user_similarity ON similar_users_cache (user_id, similarity_score DESC)",
        "CREATE INDEX IF NOT EXISTS similar_users_cache_idx_computed ON similar_users_cache (computed_at)"
    };

    /**
     * 建 agent_* 表（幂等：CREATE TABLE IF NOT EXISTS，MySQL/SQLite 双方言）。
     * 合并部署后不再用 @PostConstruct 在容器启动时抢跑，改由 DocSys 的 docSysInit
     * 成功路径经 {@link AgentInitService} 统一触发 —— 保证在 DocSys 确认数据库就绪后才建表。
     */
    public void init() {
        log.info("Starting database table initialization check...");

        try (Connection conn = dataSource.getConnection()) {
            boolean sqlite = isSQLite(conn);
            if (sqlite) {
                log.info("Connected to database: SQLite (self-contained, no schema name)");
            } else {
                String databaseName = getDatabaseName(conn);
                log.info("Connected to database: {}", databaseName);
            }

            List<String> missingTables = checkMissingTables(conn, sqlite);

            if (missingTables.isEmpty()) {
                log.info("All required DocSysAgent tables exist. Initialization complete.");
                return;
            }

            log.warn("Found {} missing tables: {}", missingTables.size(), missingTables);
            log.info("Creating missing tables...");

            int created = createMissingTables(conn, sqlite);

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
     * Detect whether the current connection is a SQLite database.
     *
     * Uses connection metadata as the primary, self-contained detection since it is
     * always correct regardless of when this component runs. DocSys's DB_TYPE field
     * (com.DocSystem.controller.BaseController.DB_TYPE) is package-protected and not
     * accessible from this package, so metadata is preferred.
     */
    private boolean isSQLite(Connection conn) {
        try {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product != null && product.toLowerCase().contains("sqlite")) {
                return true;
            }
        } catch (SQLException e) {
            log.warn("Could not read database product name, assuming MySQL: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Get the current database name from connection (MySQL only).
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
     *
     * SQLite: query sqlite_master (no information_schema / database name concept).
     * MySQL:  query information_schema.tables scoped by the current database name.
     */
    private List<String> checkMissingTables(Connection conn, boolean sqlite) throws SQLException {
        List<String> missing = new ArrayList<>();
        List<String> existing = new ArrayList<>();

        // Build placeholder list for IN clause
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < REQUIRED_TABLES.length; i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        if (sqlite) {
            String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name IN ("
                       + placeholders.toString() + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < REQUIRED_TABLES.length; i++) {
                    ps.setString(i + 1, REQUIRED_TABLES[i]);
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    existing.add(rs.getString("name"));
                }
            }
        } else {
            String databaseName = getDatabaseName(conn);
            String sql = "SELECT table_name FROM information_schema.tables "
                       + "WHERE table_schema = ? AND table_name IN ("
                       + placeholders.toString() + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, databaseName);
                for (int i = 0; i < REQUIRED_TABLES.length; i++) {
                    ps.setString(i + 2, REQUIRED_TABLES[i]);
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    existing.add(rs.getString("table_name"));
                }
            }
        }

        // Find missing tables
        for (String table : REQUIRED_TABLES) {
            if (!existing.contains(table)) {
                missing.add(table);
            }
        }

        return missing;
    }

    /**
     * Create missing tables using CREATE TABLE IF NOT EXISTS.
     *
     * For SQLite it also executes the extracted CREATE INDEX IF NOT EXISTS statements
     * afterwards (SQLite does not allow inline non-unique indexes).
     */
    private int createMissingTables(Connection conn, boolean sqlite) throws SQLException {
        int created = 0;

        String[] statements = sqlite ? CREATE_TABLE_STATEMENTS_SQLITE : CREATE_TABLE_STATEMENTS;
        for (String sql : statements) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
                created++;
                log.debug("Executed CREATE TABLE for: {}", extractTableName(sql));
            } catch (SQLException e) {
                log.warn("Failed to create table (may already exist): {}", e.getMessage());
                // Continue with other tables
            }
        }

        if (sqlite) {
            for (String sql : CREATE_INDEX_STATEMENTS_SQLITE) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.executeUpdate();
                    log.debug("Executed CREATE INDEX: {}", sql);
                } catch (SQLException e) {
                    log.warn("Failed to create index (may already exist): {}", e.getMessage());
                    // Continue with other indexes
                }
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
