package com.DocSystem.agent.memory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户记忆存储的内存实现 —— 仅用于护栏单测（T8.3）。
 */
public class InMemoryUserMemoryStore implements UserMemoryStore {

    private final Map<String, Map<String, String>> store = new ConcurrentHashMap<>();

    @Override
    public boolean set(String username, String key, String value) {
        if (username == null || username.isEmpty() || key == null || key.isEmpty()) {
            return false;
        }
        store.computeIfAbsent(username, k -> new ConcurrentHashMap<>()).put(key, value);
        return true;
    }

    @Override
    public String get(String username, String key) {
        if (username == null || username.isEmpty() || key == null || key.isEmpty()) {
            return null;
        }
        Map<String, String> userMem = store.get(username);
        return userMem == null ? null : userMem.get(key);
    }

    @Override
    public Map<String, String> list(String username) {
        Map<String, String> result = new LinkedHashMap<>();
        if (username == null || username.isEmpty()) {
            return result;
        }
        Map<String, String> userMem = store.get(username);
        if (userMem != null) {
            result.putAll(userMem);
        }
        return result;
    }

    @Override
    public boolean delete(String username, String key) {
        if (username == null || username.isEmpty() || key == null || key.isEmpty()) {
            return false;
        }
        Map<String, String> userMem = store.get(username);
        if (userMem != null) {
            userMem.remove(key);
        }
        return true;
    }
}
