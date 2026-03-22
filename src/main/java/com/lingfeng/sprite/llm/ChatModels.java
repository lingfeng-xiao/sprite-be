package com.lingfeng.sprite.llm;

import java.util.List;
import java.util.Map;

/**
 * Chat 相关的 LLM 数据模型
 */
public class ChatModels {

    /**
     * LLM 上下文
     */
    public record LlmContext(
            String selfSummary,
            String ownerSummary,
            String currentSituation,
            String chatHistory,
            String availableTools,
            String memoryHighlights
    ) {}

    /**
     * LLM 思考结果
     */
    public record LlmThought(
            String reasoning,
            String response,
            String insight,
            List<ToolCall> toolCalls,
            float confidence
    ) {}

    /**
     * 工具调用
     */
    public record ToolCall(
            String tool,
            Map<String, Object> params
    ) {}
}
