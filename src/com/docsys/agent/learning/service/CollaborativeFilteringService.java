package com.docsys.agent.learning.service;

import com.docsys.agent.learning.entity.*;
import com.docsys.agent.learning.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * CollaborativeFilteringService - 协同过滤推荐引擎
 *
 * 核心算法:
 * 1. 权限相似度计算 (Permission Similarity)
 * 2. 行为标签相似度计算 (Behavior Tag Similarity)
 * 3. 加权排序推荐 (Weighted Ranking)
 *
 * 设计原则:
 * - 缓存相似用户,避免重复计算
 * - 支持用户隔离(租户内推荐)
 * - 异步更新缓存
 */
@Service
public class CollaborativeFilteringService {
    
    private static final Logger log = LoggerFactory.getLogger(CollaborativeFilteringService.class);
    
    // 权重配置
    private static final double PERMISSION_WEIGHT = 0.4;
    private static final double BEHAVIOR_WEIGHT = 0.4;
    private static final double PROXIMITY_WEIGHT = 0.2;
    
    // 推荐权重
    private static final double SUCCESS_RATE_WEIGHT = 0.30;
    private static final double SIMILAR_USER_COVERAGE_WEIGHT = 0.30;
    private static final double PERMISSION_MATCH_WEIGHT = 0.20;
    private static final double RECENCY_WEIGHT = 0.10;
    
    // 相似用户缓存
    private final Map<String, List<SimilarUser>> similarUsersCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600000; // 1小时
    
    // MyBatis Repositories (optional)
    @Autowired(required = false)
    private UserPermissionRepository userPermissionRepository;

    @Autowired(required = false)
    private UserBehaviorTagRepository userBehaviorTagRepository;

    @Autowired(required = false)
    private UserExperienceRepository userExperienceRepository;

    @Autowired(required = false)
    private SharedKnowledgeRepository sharedKnowledgeRepository;
    
    /**
     * 推荐入口方法
     */
    public List<Recommendation> recommend(String userId, int limit) {
        log.info("Generating recommendations for user: {}", userId);
        
        // Step 1: 获取用户画像
        UserProfile currentUser = getUserProfile(userId);
        if (currentUser == null) {
            log.warn("User profile not found: {}", userId);
            return Collections.emptyList();
        }
        
        // Step 2: 找到相似用户
        List<SimilarUser> similarUsers = findSimilarUsers(currentUser);
        if (similarUsers.isEmpty()) {
            log.info("No similar users found for: {}", userId);
            return getDefaultRecommendations(currentUser);
        }
        
        // Step 3: 从相似用户提取成功经验
        List<Experience> candidates = extractExperiencesFromSimilarUsers(similarUsers, limit * 3);
        
        // Step 4: 过滤掉当前用户已有的经验
        List<Experience> newExperiences = filterOutExisting(candidates, currentUser);
        
        // Step 5: 加权排序
        List<Recommendation> recommendations = rankByWeightedScore(newExperiences, similarUsers, currentUser);
        
        // Step 6: 缓存结果
        cacheRecommendations(userId, recommendations);
        
        return recommendations.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * 计算综合相似度
     * 权限相似度(40%) + 行为相似度(40%) + 部门相近度(20%)
     */
    public double calculateSimilarityScore(UserProfile user1, UserProfile user2) {
        double permissionSim = calculatePermissionSimilarity(user1, user2);
        double behaviorSim = calculateBehaviorSimilarity(user1, user2);
        double proximitySim = calculateProximity(user1, user2);
        
        return permissionSim * PERMISSION_WEIGHT + 
               behaviorSim * BEHAVIOR_WEIGHT + 
               proximitySim * PROXIMITY_WEIGHT;
    }
    
    /**
     * 权限相似度计算
     * 相同角色=1.0, 相同部门=0.5, 权限级别差1=0.8
     */
    private double calculatePermissionSimilarity(UserProfile u1, UserProfile u2) {
        double score = 0.0;
        int matchCount = 0;
        
        // 角色匹配
        Set<String> roles1 = new HashSet<>(u1.getRoles());
        Set<String> roles2 = new HashSet<>(u2.getRoles());
        
        if (!roles1.isEmpty() && !roles2.isEmpty()) {
            Set<String> intersection = new HashSet<>(roles1);
            intersection.retainAll(roles2);
            
            if (!intersection.isEmpty()) {
                double roleSim = (double) intersection.size() / 
                               Math.max(roles1.size(), roles2.size());
                score += roleSim;
                matchCount++;
            }
        }
        
        // 权限级别相近度
        if (u1.getPermissionLevel() != null && u2.getPermissionLevel() != null) {
            int permDiff = Math.abs(u1.getPermissionLevel() - u2.getPermissionLevel());
            double permSim = permDiff == 0 ? 1.0 : (permDiff == 1 ? 0.8 : 0.5);
            score += permSim;
            matchCount++;
        }
        
        // 部门匹配
        if (u1.getDepartmentId() != null && 
            u1.getDepartmentId().equals(u2.getDepartmentId())) {
            score += 1.0;
            matchCount++;
        }
        
        return matchCount > 0 ? score / matchCount : 0.0;
    }
    
    /**
     * 行为标签相似度 (余弦相似度)
     */
    private double calculateBehaviorSimilarity(UserProfile u1, UserProfile u2) {
        List<UserBehaviorTag> tags1List = userBehaviorTagRepository.findByUserId(u1.getUserId());
        List<UserBehaviorTag> tags2List = userBehaviorTagRepository.findByUserId(u2.getUserId());
        
        if (tags1List.isEmpty() || tags2List.isEmpty()) {
            return 0.0;
        }
        
        // 转换为Map
        Map<String, Integer> tags1 = tags1List.stream()
            .collect(Collectors.toMap(UserBehaviorTag::getTag, UserBehaviorTag::getActionCount, Integer::sum));
        Map<String, Integer> tags2 = tags2List.stream()
            .collect(Collectors.toMap(UserBehaviorTag::getTag, UserBehaviorTag::getActionCount, Integer::sum));
        
        // 构建向量
        Set<String> allTags = new HashSet<>(tags1.keySet());
        allTags.addAll(tags2.keySet());
        
        double[] vec1 = new double[allTags.size()];
        double[] vec2 = new double[allTags.size()];
        
        int i = 0;
        for (String tag : allTags) {
            vec1[i] = tags1.getOrDefault(tag, 0);
            vec2[i] = tags2.getOrDefault(tag, 0);
            i++;
        }
        
        return cosineSimilarity(vec1, vec2);
    }
    
    /**
     * 余弦相似度计算
     */
    private double cosineSimilarity(double[] vec1, double[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }
    
    /**
     * 部门/租户相近度
     */
    private double calculateProximity(UserProfile u1, UserProfile u2) {
        // 相同租户
        if (u1.getTenantId() != null && u1.getTenantId().equals(u2.getTenantId())) {
            return 1.0;
        }
        return 0.0;
    }
    
    /**
     * 找到相似用户
     */
    private List<SimilarUser> findSimilarUsers(UserProfile currentUser) {
        String cacheKey = currentUser.getTenantId() + ":" + currentUser.getUserId();
        
        // 尝试从缓存获取
        List<SimilarUser> cached = similarUsersCache.get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        
        // 从数据库获取所有用户
        List<String> allUserIds;
        if (currentUser.getTenantId() != null) {
            allUserIds = userPermissionRepository.findUserIdsByTenantId(currentUser.getTenantId());
        } else {
            allUserIds = userPermissionRepository.findAllUserIds();
        }
        
        // 构建用户画像Map
        Map<String, UserProfile> otherUserProfiles = new HashMap<>();
        for (String otherUserId : allUserIds) {
            if (otherUserId.equals(currentUser.getUserId())) {
                continue; // 跳过自己
            }
            UserProfile otherUser = buildUserProfile(otherUserId);
            if (otherUser != null) {
                otherUserProfiles.put(otherUserId, otherUser);
            }
        }
        
        // 计算所有用户的相似度
        List<SimilarUser> similarUsers = new ArrayList<>();
        
        for (Map.Entry<String, UserProfile> entry : otherUserProfiles.entrySet()) {
            String otherUserId = entry.getKey();
            UserProfile otherUser = entry.getValue();
            
            double similarity = calculateSimilarityScore(currentUser, otherUser);
            if (similarity > 0.1) { // 阈值过滤
                SimilarUser similarUser = new SimilarUser(otherUserId, similarity);
                similarUser.setPermissionSimilarity(calculatePermissionSimilarity(currentUser, otherUser));
                similarUser.setBehaviorSimilarity(calculateBehaviorSimilarity(currentUser, otherUser));
                similarUsers.add(similarUser);
            }
        }
        
        // 排序取Top N
        similarUsers.sort((a, b) -> b.getSimilarityScore().compareTo(a.getSimilarityScore()));
        List<SimilarUser> topN = similarUsers.stream().limit(20).collect(Collectors.toList());
        
        // 缓存
        similarUsersCache.put(cacheKey, topN);
        
        return topN;
    }
    
    /**
     * 从数据库构建用户画像
     */
    private UserProfile buildUserProfile(String userId) {
        List<UserPermission> permissions = userPermissionRepository.findByUserId(userId);
        if (permissions.isEmpty()) {
            return null;
        }
        
        UserProfile profile = new UserProfile(userId, permissions.get(0).getTenantId());
        Set<String> roles = new HashSet<>();
        String departmentId = null;
        Integer maxPermissionLevel = 0;
        
        for (UserPermission perm : permissions) {
            if (perm.getRoleName() != null) {
                roles.add(perm.getRoleName());
            }
            if (departmentId == null && perm.getDepartmentId() != null) {
                departmentId = perm.getDepartmentId();
            }
            if (perm.getPermissionLevel() != null && perm.getPermissionLevel() > maxPermissionLevel) {
                maxPermissionLevel = perm.getPermissionLevel();
            }
        }
        
        profile.setRoles(roles);
        profile.setDepartmentId(departmentId);
        profile.setPermissionLevel(maxPermissionLevel);
        
        return profile;
    }
    
    /**
     * 从相似用户提取成功经验
     */
    private List<Experience> extractExperiencesFromSimilarUsers(List<SimilarUser> similarUsers, int limit) {
        Map<String, Experience> experienceMap = new LinkedHashMap<>();
        
        for (SimilarUser similarUser : similarUsers) {
            List<UserExperience> experiences = userExperienceRepository
                .findSuccessByUserId(similarUser.getUserId(), 50);
            
            for (UserExperience exp : experiences) {
                String key = exp.getTaskType();
                if (!experienceMap.containsKey(key)) {
                    Experience expConverted = convertToExperience(exp);
                    experienceMap.put(key, expConverted);
                }
            }
        }
        
        return experienceMap.values().stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 将UserExperience转换为Experience
     */
    private Experience convertToExperience(UserExperience userExp) {
        Experience exp = new Experience();
        exp.setIntent(userExp.getTaskType());
        exp.setSuccess(userExp.getSuccess());
        exp.setSuccessRate(userExp.getSuccess() ? 0.8 : 0.2);
        exp.setRecencyScore(calculateRecencyScore(userExp.getCreatedAt()));
        // 从taskOutput解析actions (简化实现)
        if (userExp.getTaskOutput() != null) {
            exp.setActions(Arrays.asList(userExp.getTaskOutput().split("\n")));
        }
        return exp;
    }
    
    /**
     * 计算时间衰减分数
     */
    private double calculateRecencyScore(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0.0;
        }
        LocalDateTime now = LocalDateTime.now();
        long daysSince = java.time.Duration.between(dateTime, now).toDays();
        if (daysSince <= 1) return 1.0;
        if (daysSince <= 7) return 0.8;
        if (daysSince <= 30) return 0.5;
        if (daysSince <= 90) return 0.3;
        return 0.1;
    }
    
    /**
     * 过滤掉用户已有的经验
     */
    private List<Experience> filterOutExisting(List<Experience> candidates, UserProfile currentUser) {
        List<UserExperience> userExps = userExperienceRepository.findByUserId(currentUser.getUserId());
        Set<String> userIntents = userExps.stream()
            .map(UserExperience::getTaskType)
            .collect(Collectors.toSet());
        
        return candidates.stream()
            .filter(exp -> !userIntents.contains(exp.getIntent()))
            .collect(Collectors.toList());
    }
    
    /**
     * 加权排序推荐
     */
    private List<Recommendation> rankByWeightedScore(
            List<Experience> experiences,
            List<SimilarUser> similarUsers,
            UserProfile currentUser) {
        
        // 构建相似用户Map方便查找
        Map<String, SimilarUser> similarUserMap = similarUsers.stream()
            .collect(Collectors.toMap(SimilarUser::getUserId, s -> s));
        
        return experiences.stream()
            .map(exp -> {
                double score = 0.0;
                
                // 1. 成功率高 (30%)
                score += exp.getSuccessRate() * SUCCESS_RATE_WEIGHT;
                
                // 2. 相似用户使用过 (30%)
                double coverage = calculateSimilarUserCoverage(exp, similarUserMap);
                score += coverage * SIMILAR_USER_COVERAGE_WEIGHT;
                
                // 3. 权限范围内 (20%)
                if (isWithinPermission(exp, currentUser)) {
                    score += PERMISSION_MATCH_WEIGHT;
                }
                
                // 4. 最近活跃 (10%)
                score += exp.getRecencyScore() * RECENCY_WEIGHT;
                
                // 构建推荐
                return Recommendation.builder()
                    .intent(exp.getIntent())
                    .actions(exp.getActions())
                    .confidenceScore(score)
                    .reason(buildReason(exp, similarUserMap, currentUser))
                    .sourceUserIds(extractSourceUserIds(exp, similarUserMap))
                    .build();
            })
            .sorted(Comparator.comparing(Recommendation::getConfidenceScore).reversed())
            .collect(Collectors.toList());
    }
    
    private double calculateSimilarUserCoverage(Experience exp, Map<String, SimilarUser> similarUserMap) {
        // 简化实现: 返回0.5作为默认值
        return 0.5;
    }
    
    private boolean isWithinPermission(Experience exp, UserProfile currentUser) {
        // 简化: 检查intent是否在用户权限范围内
        return true;
    }
    
    private String buildReason(Experience exp, Map<String, SimilarUser> similarUserMap, UserProfile currentUser) {
        int count = (int) similarUserMap.values().stream()
            .filter(s -> !userExperienceRepository.findByUserId(s.getUserId()).isEmpty())
            .count();
        
        if (count > 0) {
            return String.format("与您权限相似的%d位用户都使用此功能", count);
        }
        return "根据您的使用习惯推荐";
    }
    
    private List<String> extractSourceUserIds(Experience exp, Map<String, SimilarUser> similarUserMap) {
        return new ArrayList<>(similarUserMap.keySet());
    }
    
    private List<Recommendation> getDefaultRecommendations(UserProfile user) {
        // 默认推荐
        List<Recommendation> defaults = new ArrayList<>();
        
        defaults.add(Recommendation.builder()
            .intent("list_repos")
            .actions(Arrays.asList("list-repos"))
            .confidenceScore(0.5)
            .reason("查看所有仓库")
            .sourceUserIds(new ArrayList<>())
            .build());
        
        return defaults;
    }
    
    private void cacheRecommendations(String userId, List<Recommendation> recommendations) {
        // 简化: 实际应该写入MySQL
        log.debug("Cached {} recommendations for user {}", recommendations.size(), userId);
    }
    
    // ===== 数据管理方法 =====
    
    /**
     * 注册用户权限信息
     */
    @Transactional
    public void registerUser(String userId, String tenantId, String role, String departmentId, Integer permissionLevel) {
        UserPermission permission = new UserPermission();
        permission.setUserId(userId);
        permission.setTenantId(tenantId);
        permission.setRoleName(role);
        permission.setDepartmentId(departmentId);
        permission.setPermissionLevel(permissionLevel);
        permission.setCreatedAt(LocalDateTime.now());
        userPermissionRepository.insert(permission);
    }
    
    /**
     * 记录用户行为标签
     */
    @Transactional
    public void recordBehaviorTag(String userId, String tenantId, String tag) {
        UserBehaviorTag existing = userBehaviorTagRepository.findByUserIdAndTag(userId, tag);
        if (existing != null) {
            existing.setActionCount(existing.getActionCount() + 1);
            existing.setLastActionAt(LocalDateTime.now());
            userBehaviorTagRepository.updateById(existing);
        } else {
            UserBehaviorTag newTag = new UserBehaviorTag();
            newTag.setUserId(userId);
            newTag.setTenantId(tenantId);
            newTag.setTag(tag);
            newTag.setActionCount(1);
            newTag.setCreatedAt(LocalDateTime.now());
            userBehaviorTagRepository.insert(newTag);
        }
    }
    
    /**
     * 记录用户经验
     */
    @Transactional
    public void recordExperience(String userId, String tenantId, Experience exp) {
        UserExperience userExp = new UserExperience();
        userExp.setUserId(userId);
        userExp.setTenantId(tenantId);
        userExp.setTaskType(exp.getIntent());
        userExp.setTaskInput(exp.getTaskInput());
        userExp.setTaskOutput(exp.getTaskOutput() != null ? String.join("\n", exp.getActions()) : null);
        userExp.setSuccess(exp.isSuccess());
        userExp.setDurationMs(exp.getDurationMs());
        userExp.setRating(exp.getRating());
        userExp.setFeedback(exp.getFeedback());
        userExp.setTags(exp.getTags() != null ? String.join(",", exp.getTags()) : null);
        userExp.setCreatedAt(LocalDateTime.now());
        userExp.setUpdatedAt(LocalDateTime.now());
        userExperienceRepository.insert(userExp);
    }
    
    /**
     * 获取用户画像
     */
    public UserProfile getUserProfile(String userId) {
        return buildUserProfile(userId);
    }
    
    /**
     * 初始化示例数据
     */
    @Transactional
    public void initSampleData() {
        // 创建示例用户权限
        registerUser("user_001", "tenant_001", "admin", "dept_tech", 2);
        registerUser("user_002", "tenant_001", "admin", "dept_tech", 1);
        registerUser("user_003", "tenant_001", "user", "dept_sales", 0);
        
        // 添加行为标签
        recordBehaviorTag("user_001", "tenant_001", "仓库管理");
        recordBehaviorTag("user_001", "tenant_001", "搜索达人");
        recordBehaviorTag("user_001", "tenant_001", "文档上传");
        
        recordBehaviorTag("user_002", "tenant_001", "仓库管理");
        recordBehaviorTag("user_002", "tenant_001", "备份管理");
        
        recordBehaviorTag("user_003", "tenant_001", "搜索达人");
        
        // 添加经验
        Experience exp1 = new Experience();
        exp1.setIntent("create_repos");
        exp1.setSuccess(true);
        exp1.setSuccessRate(0.9);
        recordExperience("user_001", "tenant_001", exp1);
        
        log.info("Sample data initialized successfully");
    }
}
