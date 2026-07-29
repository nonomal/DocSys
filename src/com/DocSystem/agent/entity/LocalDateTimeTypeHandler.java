package com.DocSystem.agent.entity;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * MyBatis type handler for {@link LocalDateTime}.
 *
 * <p>MyBatis 3.1.1 predates the JSR-310 handlers (added in 3.4+), so it has no
 * built-in {@code TypeHandler} for {@code java.time.LocalDateTime}. Without this
 * handler, inserts/reads of the merged DocSysAgent entities (TaskEntity,
 * SessionEntity, audit/learning entities, etc.) fail with
 * {@code There was no TypeHandler found for parameter ...}.
 *
 * <p>Persistence goes through JDBC {@link Timestamp} for maximum compatibility
 * across MySQL and SQLite. On read, SQLite stores timestamps as text and
 * {@code rs.getTimestamp} may throw or mis-handle certain text formats, so the
 * read paths fall back to parsing the raw string when needed.
 */
@MappedTypes(LocalDateTime.class)
public class LocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setTimestamp(i, Timestamp.valueOf(parameter));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        try {
            Timestamp ts = rs.getTimestamp(columnName);
            return ts == null ? null : ts.toLocalDateTime();
        } catch (SQLException e) {
            return parse(rs.getString(columnName));
        }
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            Timestamp ts = rs.getTimestamp(columnIndex);
            return ts == null ? null : ts.toLocalDateTime();
        } catch (SQLException e) {
            return parse(rs.getString(columnIndex));
        }
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            Timestamp ts = cs.getTimestamp(columnIndex);
            return ts == null ? null : ts.toLocalDateTime();
        } catch (SQLException e) {
            return parse(cs.getString(columnIndex));
        }
    }

    /**
     * Parse a raw DB string into a LocalDateTime, tolerating both MySQL and
     * SQLite text storage formats. Returns null if the value cannot be parsed.
     */
    private LocalDateTime parse(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        // Handles "yyyy-MM-dd HH:mm:ss[.fff]".
        try {
            return Timestamp.valueOf(normalized).toLocalDateTime();
        } catch (IllegalArgumentException ignore) {
            // fall through to ISO parsing
        }
        // Handles "yyyy-MM-ddTHH:mm:ss" (ISO-8601) after swapping the space.
        try {
            return LocalDateTime.parse(normalized.replace(' ', 'T'));
        } catch (RuntimeException ignore) {
            return null;
        }
    }
}
