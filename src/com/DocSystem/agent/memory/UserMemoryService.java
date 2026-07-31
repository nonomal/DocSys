package com.DocSystem.agent.memory;

import com.DocSystem.agent.repository.UserMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户记忆服务（T8.3）—— {@link UserMemoryStore} 的数据库实现。
 *
 * <p>跨会话持久化用户偏好/上下文到 agent_user_memory 表。供 memory_set/memory_get/memory_list
 * 工具使用。DB 失败只记日志不向上抛（不影响主流程），返回 false/null 由工具转成错误结果。</p>
 */
@Service
public class UserMemoryService implements UserMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(UserMemoryService.class);

    @Autowired
    private UserMemoryRepository repository;

    /** 供测试注入 mock repository */
    public void setRepository(UserMemoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean set(String username, String key, String value) {
        if (username == null || username.isEmpty() || key == null || key.isEmpty()) {
            return false;
        }
        try {
            // 两步法（兼容 MariaDB/SQLite）：先查后 insert/update
            UserMemoryEntity existing = repository.selectByKey(username, key);
            if (existing != null) {
                return repository.update(username, key, value) > 0;
            }
            return repository.insert(username, key, value) > 0;
        } catch (Exception e) {
            log.warn("UserMemoryService.set failed: username={}, key={}, err={}", username, key, e.getMessage());
            return false;
        }
    }

    @Override
    public String get(String username, String key) {
        if (username == null || username.isEmpty() || key == null || key.isEmpty()) {
            return null;
        }
        try {
            UserMemoryEntity entity = repository.selectByKey(username, key);
            return entity == null ? null : entity.getMemValue();
        } catch (Exception e) {
            log.warn("UserMemoryService.get failed: username={}, key={}, err={}", username, key, e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, String> list(String username) {
        Map<String, String> result = new LinkedHashMap<>();
        if (username == null || username.isEmpty()) {
            return result;
        }
        try {
            List<UserMemoryEntity> list = repository.selectByUsername(username);
            if (list != null) {
                for (UserMemoryEntity e : list) {
                    result.put(e.getMemKey(), e.getMemValue());
                }
            }
        } catch (Exception ex) {
            log.warn("UserMemoryService.list failed: username={}, err={}", username, ex.getMessage());
        }
        return result;
    }

    @Override
    public boolean delete(String username, String key) {
        if (username == null || username.isEmpty() || key == null || key.isEmpty()) {
            return false;
        }
        try {
            repository.deleteByKey(username, key);
            return true;
        } catch (Exception e) {
            log.warn("UserMemoryService.delete failed: username={}, key={}, err={}", username, key, e.getMessage());
            return false;
        }
    }
}
