package com.DocSystem.agent.entity;

import com.DocSystem.agent.entity.TaskEntity.TaskStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Case-insensitive MyBatis type handler for {@link TaskStatus}.
 *
 * <p>The default {@code EnumTypeHandler} maps via {@code Enum.valueOf(name)},
 * which is case-sensitive: a DB column declared as
 * {@code ENUM('pending','running',...)} (lowercase) makes reads throw
 * {@code IllegalArgumentException: No enum constant TaskStatus.pending}.
 *
 * <p>This handler normalizes to UPPERCASE before comparing on read, and always
 * writes the canonical {@code name()} (uppercase) on write — so persistence is
 * robust regardless of how the underlying column's ENUM labels were cased.
 */
@MappedTypes(TaskStatus.class)
public class TaskStatusTypeHandler extends BaseTypeHandler<TaskStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TaskStatus parameter, JdbcType jdbcType)
            throws SQLException {
        // Always persist the canonical uppercase name.
        ps.setString(i, parameter.name());
    }

    @Override
    public TaskStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public TaskStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public TaskStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    /** Parse a DB string into a TaskStatus, tolerating any letter casing / surrounding whitespace. */
    private TaskStatus parse(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return TaskStatus.valueOf(normalized.toUpperCase());
    }
}
