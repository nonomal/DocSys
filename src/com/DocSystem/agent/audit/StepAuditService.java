package com.DocSystem.agent.audit;

import com.DocSystem.agent.repository.StepAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Step 审计服务（T8.5）—— 把工具链每一步的结构化审计落库 agent_step_audits 表。
 *
 * <p>由 MainAgent 构造 {@link StepAuditSink} 闭包调用。失败只记日志不抛（不影响主流程）。</p>
 */
@Service
public class StepAuditService {

    private static final Logger log = LoggerFactory.getLogger(StepAuditService.class);

    /** 参数/结果摘要截断长度（防上下文膨胀落库） */
    private static final int MAX_SUMMARY_LEN = 500;

    @Autowired
    private StepAuditRepository repository;

    /** 供测试注入 mock repository */
    public void setRepository(StepAuditRepository repository) {
        this.repository = repository;
    }

    /**
     * 记录一步工具审计。
     *
     * @param requestId    请求 ID（MDC requestId / traceId）
     * @param sessionId    Agent 会话 ID
     * @param turn         轮次（从 1 开始）
     * @param tool         工具名
     * @param argsJson     参数 JSON（可为 null）
     * @param resultSummary 结果摘要（成功摘要或错误信息，可为 null）
     * @param success      是否成功
     * @param durationMs   耗时（毫秒）
     */
    public void record(String requestId, String sessionId, Integer turn, String tool,
                       String argsJson, String resultSummary, boolean success, long durationMs) {
        try {
            if (tool == null || tool.isEmpty()) {
                return;
            }
            StepAuditEntity entity = new StepAuditEntity(
                    requestId != null ? requestId : "",
                    sessionId != null ? sessionId : "",
                    turn != null ? turn : 0,
                    tool,
                    truncate(argsJson),
                    truncate(resultSummary),
                    success,
                    durationMs);
            repository.insert(entity);
        } catch (Exception e) {
            // 审计失败绝不向上抛，避免打断工具链主流程
            log.warn("StepAuditService.record failed: tool={}, err={}", tool, e.getMessage());
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() > MAX_SUMMARY_LEN) {
            return s.substring(0, MAX_SUMMARY_LEN) + "...(truncated)";
        }
        return s;
    }
}
