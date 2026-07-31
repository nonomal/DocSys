package com.DocSystem.agent.memory;

import java.util.Map;

/**
 * 用户记忆存储抽象（T8.3）。
 *
 * <p>跨会话持久化用户偏好/上下文的读写接口。Spring 实现为 {@link UserMemoryService}
 * （落库 agent_user_memory 表）；测试可用 {@link InMemoryUserMemoryStore}。</p>
 */
public interface UserMemoryStore {

    /**
     * 写入一条用户记忆（同 key 覆盖）。
     *
     * @return true=成功；false=参数非法或存储失败
     */
    boolean set(String username, String key, String value);

    /**
     * 读取一条用户记忆。
     *
     * @return 记忆值；不存在或失败 → null
     */
    String get(String username, String key);

    /**
     * 列出某用户全部记忆（key → value）。
     */
    Map<String, String> list(String username);

    /**
     * 删除一条用户记忆。
     *
     * @return true=删除成功（含不存在视为成功）；false=参数非法或存储失败
     */
    boolean delete(String username, String key);
}
