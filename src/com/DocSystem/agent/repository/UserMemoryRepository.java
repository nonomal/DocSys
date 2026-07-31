package com.DocSystem.agent.repository;

import com.DocSystem.agent.memory.UserMemoryEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper for agent_user_memory table（T8.3）。
 * 包位于 com.DocSystem.agent.repository —— 已被 MapperScannerConfigurer 扫描。
 *
 * <p>写入采用「select→insert/update 两步法」而非 upsert 语法，以兼容
 * MariaDB 与 SQLite 两种方言（两者 upsert 语法不兼容）。</p>
 */
public interface UserMemoryRepository {

    /** 新增一条记忆（同 username+mem_key 冲突时由服务层先查再走 update） */
    int insert(@Param("username") String username,
               @Param("memKey") String memKey,
               @Param("memValue") String memValue);

    /** 更新一条记忆 */
    int update(@Param("username") String username,
               @Param("memKey") String memKey,
               @Param("memValue") String memValue);

    /** 某用户全部记忆（按 mem_key 升序） */
    List<UserMemoryEntity> selectByUsername(@Param("username") String username);

    /** 单条记忆（不存在 → null） */
    UserMemoryEntity selectByKey(@Param("username") String username,
                                 @Param("memKey") String memKey);

    /** 删除单条记忆 */
    int deleteByKey(@Param("username") String username,
                    @Param("memKey") String memKey);
}
