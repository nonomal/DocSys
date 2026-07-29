package com.DocSystem.agent.learning.service;

import com.DocSystem.agent.learning.entity.UserBehaviorTag;
import com.DocSystem.agent.learning.repository.UserBehaviorTagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BehaviorTrackingService - 行为追踪服务
 * 
 * 功能:
 * 1. 自动追踪用户行为并打标签
 * 2. 根据操作类型推断标签
 * 3. 提供行为分析统计
 */
@Service
public class BehaviorTrackingService {
    
    private static final Logger log = LoggerFactory.getLogger(BehaviorTrackingService.class);
    
    @Autowired(required = false)
    private UserBehaviorTagRepository behaviorTagRepository;
    
    @Autowired(required = false)
    private CollaborativeFilteringService learningService;
    
    // 操作类型到标签的映射
    private static final Map<String, List<String>> ACTION_TO_TAGS = new HashMap<>();
    static {
        // 仓库相关
        ACTION_TO_TAGS.put("create_repos", Arrays.asList("仓库管理", "创建操作"));
        ACTION_TO_TAGS.put("delete_repos", Arrays.asList("仓库管理", "删除操作"));
        ACTION_TO_TAGS.put("update_repos", Arrays.asList("仓库管理", "修改操作"));
        ACTION_TO_TAGS.put("list_repos", Arrays.asList("仓库管理", "浏览"));
        
        // 文档相关
        ACTION_TO_TAGS.put("add_doc", Arrays.asList("文档上传", "文件操作"));
        ACTION_TO_TAGS.put("delete_doc", Arrays.asList("文档管理", "删除操作"));
        ACTION_TO_TAGS.put("rename_doc", Arrays.asList("文档管理", "修改操作"));
        ACTION_TO_TAGS.put("move_doc", Arrays.asList("文档管理", "整理"));
        ACTION_TO_TAGS.put("copy_doc", Arrays.asList("文档管理", "备份"));
        
        // 搜索相关
        ACTION_TO_TAGS.put("search", Arrays.asList("搜索达人", "文档检索"));
        
        // 分享相关
        ACTION_TO_TAGS.put("create_share", Arrays.asList("分享管理", "协作"));
        ACTION_TO_TAGS.put("share_list", Arrays.asList("分享管理", "浏览"));
        
        // 备份相关
        ACTION_TO_TAGS.put("backup", Arrays.asList("备份管理", "系统操作"));
        ACTION_TO_TAGS.put("backup_status", Arrays.asList("备份管理", "监控"));
        
        // 版本控制
        ACTION_TO_TAGS.put("doc_history", Arrays.asList("版本控制", "文档管理"));
        
        // 权限相关
        ACTION_TO_TAGS.put("lock_doc", Arrays.asList("权限管理", "文档控制"));
        ACTION_TO_TAGS.put("unlock_doc", Arrays.asList("权限管理", "文档控制"));
    }
    
    /**
     * 追踪用户行为
     * 
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param action 操作类型 (如 "create_repos", "search" 等)
     * @param metadata 额外元数据
     */
    public void trackBehavior(String userId, String tenantId, String action, Map<String, Object> metadata) {
        log.debug("Tracking behavior: userId={}, action={}", userId, action);
        
        // 根据操作类型获取标签
        List<String> tags = ACTION_TO_TAGS.getOrDefault(action, Arrays.asList(action));
        
        // 记录每个标签
        for (String tag : tags) {
            try {
                learningService.recordBehaviorTag(userId, tenantId, tag);
            } catch (Exception e) {
                log.error("Error recording behavior tag: {}", tag, e);
            }
        }
        
        // 记录具体文档类型的标签
        if (metadata != null && metadata.containsKey("docType")) {
            String docType = (String) metadata.get("docType");
            String docTypeTag = getDocTypeTag(docType);
            if (docTypeTag != null) {
                try {
                    learningService.recordBehaviorTag(userId, tenantId, docTypeTag);
                } catch (Exception e) {
                    log.error("Error recording doc type tag: {}", docTypeTag, e);
                }
            }
        }
    }
    
    /**
     * 追踪任务执行结果
     * 
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param taskType 任务类型
     * @param success 是否成功
     * @param durationMs 执行时长
     */
    public void trackTaskResult(String userId, String tenantId, String taskType, boolean success, Long durationMs) {
        log.debug("Tracking task result: userId={}, taskType={}, success={}", userId, taskType, success);
        
        // 记录经验
        Experience exp = new Experience();
        exp.setIntent(taskType);
        exp.setSuccess(success);
        exp.setSuccessRate(success ? 0.9 : 0.1);
        exp.setDurationMs(durationMs);
        
        try {
            learningService.recordExperience(userId, tenantId, exp);
        } catch (Exception e) {
            log.error("Error recording experience", e);
        }
    }
    
    /**
     * 获取文档类型对应的标签
     */
    private String getDocTypeTag(String docType) {
        if (docType == null) return null;
        String lower = docType.toLowerCase();
        if ("pdf".equals(lower)) {
            return "PDF处理";
        } else if ("word".equals(lower) || "doc".equals(lower) || "docx".equals(lower)) {
            return "Word处理";
        } else if ("excel".equals(lower) || "xls".equals(lower) || "xlsx".equals(lower)) {
            return "Excel处理";
        } else if ("ppt".equals(lower) || "pptx".equals(lower)) {
            return "PPT处理";
        } else if ("txt".equals(lower)) {
            return "文本处理";
        } else if ("image".equals(lower) || "jpg".equals(lower) || "jpeg".equals(lower) || "png".equals(lower) || "gif".equals(lower)) {
            return "图片处理";
        }
        return null;
    }
    
    /**
     * 获取用户行为统计
     */
    public Map<String, Integer> getUserBehaviorStats(String userId) {
        List<UserBehaviorTag> tags = behaviorTagRepository.findByUserId(userId);
        
        Map<String, Integer> stats = new HashMap<>();
        for (UserBehaviorTag tag : tags) {
            stats.put(tag.getTag(), tag.getActionCount());
        }
        
        return stats;
    }
}
