package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.llm.ChatModels;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;

/**
 * 对话服务
 *
 * 管理对话历史，构建 LLM 上下文，处理聊天消息
 */
@Service
public class ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    private static final int MAX_HISTORY = 50;

    private final MinMaxLlmReasoner llmReasoner;
    private final ActionExecutor actionExecutor;
    private final UnifiedContextService unifiedContextService;

    // 对话历史 (sessionId -> 历史)
    private final Map<String, List<ChatMessage>> chatHistories = new ConcurrentHashMap<>();

    public ConversationService(
            MinMaxLlmReasoner llmReasoner,
            ActionExecutor actionExecutor,
            UnifiedContextService unifiedContextService
    ) {
        this.llmReasoner = llmReasoner;
        this.actionExecutor = actionExecutor;
        this.unifiedContextService = unifiedContextService;
    }

    public record ChatMessage(String role, String content, Instant timestamp) {}

    public record ConversationResponse(String response, List<String> actions, boolean success) {}

    /**
     * 处理用户消息
     */
    public ConversationResponse chat(String userMessage, String sessionId) {
        try {
            logger.info("Processing chat message from session {}: {}", sessionId, userMessage);

            // 1. 添加用户消息到历史
            addToHistory(sessionId, "user", userMessage);

            // 2. 构建 LLM 上下文
            ChatModels.LlmContext ctx = buildContext(sessionId);

            // 3. 调用 LLM
            ChatModels.LlmThought thought = llmReasoner.think(ctx, userMessage).join();

            // 4. 解析并执行工具调用
            List<String> actionResults = executeToolCalls(thought.toolCalls());

            // 5. 组合回复
            String response = buildResponse(thought, actionResults);

            // 6. 添加助手回复到历史
            addToHistory(sessionId, "assistant", response);

            // 7. 记录对话反馈到进化引擎
            unifiedContextService.recordFeedback(sessionId, userMessage, response, true);

            logger.info("Chat response: {}", response);
            return new ConversationResponse(response, actionResults, true);

        } catch (Exception e) {
            logger.error("Error processing chat: {}", e.getMessage(), e);
            return new ConversationResponse("抱歉，处理消息时出错: " + e.getMessage(), List.of(), false);
        }
    }

    /**
     * 添加消息到历史
     */
    private void addToHistory(String sessionId, String role, String content) {
        List<ChatMessage> history = chatHistories.computeIfAbsent(sessionId, k -> new ArrayList<>());
        history.add(new ChatMessage(role, content, Instant.now()));

        // 限制历史长度
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    /**
     * 构建 LLM 上下文
     */
    private ChatModels.LlmContext buildContext(String sessionId) {
        List<ChatMessage> history = chatHistories.getOrDefault(sessionId, List.of());
        String chatHistory = formatChatHistory(history);
        String selfSummary = buildSelfSummary();
        String ownerSummary = buildOwnerSummary();
        String currentSituation = buildCurrentSituation();
        String availableTools = buildAvailableTools();
        String memoryHighlights = buildMemoryHighlights();

        return new ChatModels.LlmContext(
                selfSummary,
                ownerSummary,
                currentSituation,
                chatHistory,
                availableTools,
                memoryHighlights
        );
    }

    private String formatChatHistory(List<ChatMessage> history) {
        if (history.isEmpty()) {
            return "（无历史对话）";
        }

        StringBuilder sb = new StringBuilder();
        // 只取最近10条
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            String role = msg.role().equals("user") ? "主人" : "我";
            sb.append(role).append(": ").append(msg.content()).append("\n");
        }
        return sb.toString();
    }

    private String buildSelfSummary() {
        return unifiedContextService.buildSelfSummary();
    }

    private String buildOwnerSummary() {
        return unifiedContextService.buildOwnerSummary();
    }

    private String buildCurrentSituation() {
        return unifiedContextService.buildCurrentSituation();
    }

    private String buildAvailableTools() {
        return """
            可用工具：
            - SearchFiles(query, path): 搜索文件
            - Calculator(expression): 计算数学表达式
            - Remember(content, memoryType): 存储重要信息到记忆
            - RecallMemory(query): 搜索相关记忆
            - Notify(message, priority): 发送系统通知
            - LogAction(content): 记录日志

            如果需要调用工具，请以JSON格式返回：
            {"tool_calls": [{"tool": "工具名", "params": {"参数": "值"}}]}
            """;
    }

    private String buildMemoryHighlights() {
        return unifiedContextService.buildMemoryHighlights();
    }

    /**
     * 执行工具调用
     */
    private List<String> executeToolCalls(List<ChatModels.ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        for (ChatModels.ToolCall call : toolCalls) {
            try {
                Map<String, Object> params = call.params() != null ? call.params() : Map.of();
                var result = actionExecutor.executeTool(call.tool(), params);
                results.add(call.tool() + ": " + (result.success() ? result.message() : result.message()));
            } catch (Exception e) {
                logger.error("Error executing tool {}: {}", call.tool(), e.getMessage());
                results.add(call.tool() + ": 执行失败 - " + e.getMessage());
            }
        }
        return results;
    }

    /**
     * 构建回复文本
     */
    private String buildResponse(ChatModels.LlmThought thought, List<String> actionResults) {
        StringBuilder sb = new StringBuilder();

        if (thought.response() != null && !thought.response().isEmpty()) {
            sb.append(thought.response());
        } else if (thought.reasoning() != null && !thought.reasoning().isEmpty()) {
            sb.append(thought.reasoning());
        } else {
            sb.append("我明白了。");
        }

        if (!actionResults.isEmpty()) {
            sb.append("\n\n（已执行动作：");
            for (String result : actionResults) {
                sb.append("\n- ").append(result);
            }
            sb.append("）");
        }

        return sb.toString();
    }
}
