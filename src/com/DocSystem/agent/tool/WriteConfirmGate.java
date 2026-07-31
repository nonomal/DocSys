package com.DocSystem.agent.tool;

import com.alibaba.fastjson.JSONObject;

/**
 * 写操作确认门 —— 在 {@code needsConfirm=true} 的工具执行前拦截，等待用户批准。
 *
 * <p>抽象的目的：ToolRegistry/工具本身不关心确认机制的实现——</p>
 * <ul>
 *   <li>Web/SSE 路径：{@code AuditWriteConfirmGate} 创建 PENDING 审计 + 轮询等待用户经 /confirm 批准。</li>
 *   <li>测试/非交互路径：{@link #NOOP} 直接放行。</li>
 * </ul>
 */
@FunctionalInterface
public interface WriteConfirmGate {

    /**
     * 请求执行写操作前调用。
     *
     * @param toolName 工具名（作为审计 operation）
     * @param args     工具参数（进入审计 params）
     * @return true=允许执行；false=用户拒绝/超时
     */
    boolean confirm(String toolName, JSONObject args) throws Exception;

    /** 无确认（测试/无审计环境直接放行） */
    WriteConfirmGate NOOP = (name, args) -> true;
}
