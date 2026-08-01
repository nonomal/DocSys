package com.DocSystem.agent.repository;

import com.DocSystem.agent.config.AgentConfigEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper for agent_config table（T8.6）。
 * 包位于 com.DocSystem.agent.repository —— 已被 MapperScannerConfigurer 扫描。
 *
 * <p>写入用「select→insert/update 两步法」而非 upsert（兼容 MariaDB 与 SQLite 方言）。</p>
 */
public interface AgentConfigRepository {

    int insert(AgentConfigEntity entity);

    int update(@Param("scope") String scope,
               @Param("configKey") String configKey,
               @Param("configValue") String configValue);

    /** 单条配置（不存在 → null） */
    AgentConfigEntity selectByKey(@Param("scope") String scope,
                                  @Param("configKey") String configKey);

    /** 某作用域全部配置 */
    List<AgentConfigEntity> selectByScope(@Param("scope") String scope);
}
