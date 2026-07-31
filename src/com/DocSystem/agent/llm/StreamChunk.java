package com.DocSystem.agent.llm;

/**
 * 流式响应分片 —— 携带类型（text / reasoning / done）与内容。
 *
 * <p>T7 流式体验升级：LLM 流式输出逐 token 到达时，reasoning（思考过程）与
 * content（正文）必须分离，前端才能把 reasoning 以灰色小字展示、正文实时追加。</p>
 *
 * <p>类型说明：</p>
 * <ul>
 *   <li>{@link #TYPE_TEXT}：正文分片（最终回答 / 工具调用中间文本）</li>
 *   <li>{@link #TYPE_REASONING}：思考过程分片（OpenAI 兼容流 reasoning_content/reasoning）</li>
 *   <li>{@link #TYPE_DONE}：流结束标记（保证迭代器末尾必有）</li>
 * </ul>
 */
public final class StreamChunk {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_REASONING = "reasoning";
    public static final String TYPE_DONE = "done";

    /** 分片类型（text/reasoning/done） */
    public final String type;

    /** 分片内容（done 时为 ""） */
    public final String content;

    private StreamChunk(String type, String content) {
        this.type = type;
        this.content = content != null ? content : "";
    }

    public static StreamChunk text(String content) {
        return new StreamChunk(TYPE_TEXT, content);
    }

    public static StreamChunk reasoning(String content) {
        return new StreamChunk(TYPE_REASONING, content);
    }

    public static StreamChunk done() {
        return new StreamChunk(TYPE_DONE, "");
    }

    public boolean isText() { return TYPE_TEXT.equals(type); }
    public boolean isReasoning() { return TYPE_REASONING.equals(type); }
    public boolean isDone() { return TYPE_DONE.equals(type); }

    @Override
    public String toString() {
        return "StreamChunk{" + type + ":" + (content != null && content.length() > 40
                ? content.substring(0, 40) + "..." : content) + "}";
    }
}
