package com.docsys.agent.learning.controller;

import com.docsys.agent.learning.entity.*;
import com.docsys.agent.learning.service.CollaborativeFilteringService;
import com.docsys.agent.learning.service.Experience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Learning Controller - 学习系统REST API
 * 
 * 提供协同过滤推荐、用户画像、经验分享等API
 */
@RestController
@RequestMapping("/agent/api/learning")
public class LearningController {
    
    private static final Logger log = LoggerFactory.getLogger(LearningController.class);
    
    @Autowired
    private CollaborativeFilteringService learningService;
    
    /**
     * 获取推荐
     * GET /api/learning/recommend?userId=xxx&limit=10
     */
    @GetMapping("/recommend")
    public ResponseEntity<Map<String, Object>> getRecommendations(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting recommendations for user: {}, limit: {}", userId, limit);
        
        try {
            List<Recommendation> recommendations = learningService.recommend(userId, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("userId", userId);
            response.put("recommendations", recommendations);
            response.put("count", recommendations.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting recommendations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>(){{
                put("status", "error");
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * 注册用户权限
     * POST /api/learning/user/register
     * {
     *   "userId": "xxx",
     *   "tenantId": "xxx",
     *   "role": "admin",
     *   "departmentId": "dept_tech",
     *   "permissionLevel": 2
     * }
     */
    @PostMapping("/user/register")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");
        String tenantId = (String) request.get("tenantId");
        String role = (String) request.get("role");
        String departmentId = (String) request.get("departmentId");
        Integer permissionLevel = (Integer) request.get("permissionLevel");

        log.info("Registering user: {}, tenant: {}, role: {}", userId, tenantId, role);

        try {
            learningService.registerUser(userId, tenantId, role, departmentId, permissionLevel);

            return ResponseEntity.ok(new HashMap<String, Object>(){{
                put("status", "ok");
                put("message", "User registered successfully");
            }});
        } catch (Exception e) {
            log.error("Error registering user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>(){{
                put("status", "error");
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * 记录用户行为标签
     * POST /api/learning/behavior/tag
     * {
     *   "userId": "xxx",
     *   "tenantId": "xxx",
     *   "tag": "仓库管理"
     * }
     */
    @PostMapping("/behavior/tag")
    public ResponseEntity<Map<String, Object>> recordBehaviorTag(@RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");
        String tenantId = (String) request.get("tenantId");
        String tag = (String) request.get("tag");

        log.debug("Recording behavior tag: {} for user: {}", tag, userId);

        try {
            learningService.recordBehaviorTag(userId, tenantId, tag);

            return ResponseEntity.ok(new HashMap<String, Object>(){{
                put("status", "ok");
                put("message", "Tag recorded successfully");
            }});
        } catch (Exception e) {
            log.error("Error recording behavior tag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>(){{
                put("status", "error");
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * 记录用户经验
     * POST /api/learning/experience
     * {
     *   "userId": "xxx",
     *   "tenantId": "xxx",
     *   "intent": "create_repos",
     *   "success": true,
     *   "taskInput": "...",
     *   "taskOutput": "..."
     * }
     */
    @PostMapping("/experience")
    public ResponseEntity<Map<String, Object>> recordExperience(@RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");
        String tenantId = (String) request.get("tenantId");
        String intent = (String) request.get("intent");
        Boolean success = (Boolean) request.get("success");
        String taskInput = (String) request.get("taskInput");
        String taskOutput = (String) request.get("taskOutput");
        Integer rating = (Integer) request.get("rating");

        log.info("Recording experience for user: {}, intent: {}, success: {}", userId, intent, success);

        try {
            Experience exp = new Experience();
            exp.setIntent(intent);
            exp.setSuccess(success);
            exp.setTaskInput(taskInput);
            exp.setTaskOutput(taskOutput);
            exp.setRating(rating);
            exp.setSuccessRate(success ? 0.9 : 0.1);

            learningService.recordExperience(userId, tenantId, exp);

            return ResponseEntity.ok(new HashMap<String, Object>(){{
                put("status", "ok");
                put("message", "Experience recorded successfully");
            }});
        } catch (Exception e) {
            log.error("Error recording experience", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>(){{
                put("status", "error");
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * 获取用户画像
     * GET /api/learning/profile?userId=xxx
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@RequestParam String userId) {

        log.debug("Getting profile for user: {}", userId);

        try {
            UserProfile profile = learningService.getUserProfile(userId);

            if (profile == null) {
                return ResponseEntity.ok(new HashMap<String, Object>(){{
                    put("status", "ok");
                    put("userId", userId);
                    put("exists", false);
                }});
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("userId", profile.getUserId());
            response.put("tenantId", profile.getTenantId());
            response.put("username", profile.getUsername());
            response.put("roles", profile.getRoles());
            response.put("departmentId", profile.getDepartmentId());
            response.put("permissionLevel", profile.getPermissionLevel());
            response.put("exists", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>(){{
                put("status", "error");
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * 初始化示例数据
     * POST /api/learning/init
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initSampleData() {

        log.info("Initializing sample data");

        try {
            learningService.initSampleData();

            return ResponseEntity.ok(new HashMap<String, Object>(){{
                put("status", "ok");
                put("message", "Sample data initialized successfully");
            }});
        } catch (Exception e) {
            log.error("Error initializing sample data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>(){{
                put("status", "error");
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * 健康检查
     * GET /api/learning/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(new HashMap<String, Object>(){{
            put("status", "ok");
            put("service", "learning");
            put("timestamp", System.currentTimeMillis());
        }});
    }
}
