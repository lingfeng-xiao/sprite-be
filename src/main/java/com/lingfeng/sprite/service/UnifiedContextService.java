package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;

/**
 * 统一上下文服务
 *
 * 整合 Sprite 的感知、记忆、进化状态，为 ConversationService 提供统一的上下文访问。
 * 这是对话系统与认知循环之间的桥梁。
 *
 * 职责：
 * - 提供当前感知状态（平台、用户、环境）
 * - 提供自我模型（身份、价值观、能力）
 * - 提供世界模型（主人画像）
 * - 提供记忆访问（工作记忆、长期记忆）
 * - 记录对话反馈到进化引擎
 */
@Service
public class UnifiedContextService {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedContextService.class);

    // 上下文快照
    private volatile PerceptionSystem.Perception lastPerception;
    private volatile SelfModel.Self lastSelfModel;
    private volatile WorldModel.World lastWorldModel;
    private volatile MemorySystem.Memory memory;
    private volatile EvolutionEngine.Engine evolutionEngine;

    // 对话反馈队列（用于异步处理）
    private final ConcurrentMap<String, ConversationFeedback> feedbackQueue = new ConcurrentHashMap<>();

    // 对话历史记录（用于上下文优化）
    private final ConcurrentMap<String, ChatMessage> chatHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 50;

    public record ConversationFeedback(
            String sessionId,
            String userMessage,
            String assistantResponse,
            boolean success,
            Instant timestamp
    ) {}

    public record ChatMessage(String role, String content, Instant timestamp) {}

    /**
     * 更新上下文（由 SpriteService 定期调用）
     */
    public void updateContext(
            PerceptionSystem.Perception perception,
            SelfModel.Self selfModel,
            WorldModel.World worldModel,
            MemorySystem.Memory memory,
            EvolutionEngine.Engine evolutionEngine
    ) {
        this.lastPerception = perception;
        this.lastSelfModel = selfModel;
        this.lastWorldModel = worldModel;
        this.memory = memory;
        this.evolutionEngine = evolutionEngine;

        logger.debug("Context updated: perception={}, selfModel={}, worldModel={}",
                perception != null,
                selfModel != null,
                worldModel != null);
    }

    /**
     * 获取当前感知
     */
    public PerceptionSystem.Perception getPerception() {
        return lastPerception;
    }

    /**
     * 获取自我模型
     */
    public SelfModel.Self getSelfModel() {
        return lastSelfModel;
    }

    /**
     * 获取世界模型
     */
    public WorldModel.World getWorldModel() {
        return lastWorldModel;
    }

    /**
     * 获取记忆系统
     */
    public MemorySystem.Memory getMemory() {
        return memory;
    }

    /**
     * 获取进化引擎
     */
    public EvolutionEngine.Engine getEvolutionEngine() {
        return evolutionEngine;
    }

    /**
     * 记录对话反馈
     *
     * @param sessionId 会话 ID
     * @param userMessage 用户消息
     * @param assistantResponse 助手回复
     * @param success 是否成功
     */
    public void recordFeedback(String sessionId, String userMessage, String assistantResponse, boolean success) {
        ConversationFeedback feedback = new ConversationFeedback(
                sessionId,
                userMessage,
                assistantResponse,
                success,
                Instant.now()
        );

        feedbackQueue.put(sessionId, feedback);

        // 如果有进化引擎，记录反馈
        if (evolutionEngine != null) {
            try {
                evolutionEngine.collectFeedback(
                        new EvolutionEngine.Feedback.OwnerFeedback(
                                Instant.now(),
                                buildFeedbackContent(feedback),
                                success ? 0.5f : -0.5f,
                                null
                        )
                );
                logger.debug("Conversation feedback recorded for session {}", sessionId);
            } catch (Exception e) {
                logger.error("Failed to record conversation feedback: {}", e.getMessage());
            }
        }
    }

    /**
     * 构建反馈内容文本
     */
    private String buildFeedbackContent(ConversationFeedback feedback) {
        return String.format(
                "[对话反馈] 会话: %s | 用户: %s | 成功: %s",
                feedback.sessionId(),
                feedback.userMessage(),
                feedback.success()
        );
    }

    /**
     * 获取当前情境描述（用于 LLM 上下文）
     */
    public String buildCurrentSituation() {
        StringBuilder sb = new StringBuilder();

        if (lastPerception != null) {
            // 平台状态
            if (lastPerception.platform() != null) {
                var platform = lastPerception.platform();
                sb.append("系统状态：");
                if (platform.memory() != null) {
                    sb.append("内存使用").append(String.format("%.1f", platform.memory().usedPercent())).append("%。");
                }
                if (platform.cpu() != null) {
                    sb.append("CPU负载").append(String.format("%.1f", platform.cpu().loadPercent())).append("%。");
                }
            }

            // 用户状态
            if (lastPerception.user() != null) {
                var user = lastPerception.user();
                sb.append("用户状态：");
                sb.append("存在状态=").append(user.presence()).append("。");
                if (user.activeWindow() != null) {
                    sb.append("当前窗口=").append(user.activeWindow().title());
                    sb.append("(").append(user.activeWindow().processName()).append(")。");
                }
            }

            // 环境状态
            if (lastPerception.environment() != null) {
                var env = lastPerception.environment();
                sb.append("时间：").append(env.hourOfDay()).append("点。");
                if (env.context() != null) {
                    sb.append("情境：").append(env.context()).append("。");
                }
            }
        }

        if (sb.isEmpty()) {
            sb.append("当前系统运行正常。");
        }

        return sb.toString();
    }

    /**
     * 获取自我描述（用于 LLM 上下文）
     */
    public String buildSelfSummary() {
        SelfModel.Self self = lastSelfModel;
        if (self == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("我是").append(self.identity().displayName()).append("。");
        sb.append("我的本质是：").append(self.identity().essence()).append("。");
        sb.append("我的进化等级：").append(self.evolutionLevel()).append("（进化次数：").append(self.evolutionCount()).append("）。");
        sb.append("我的价值观：");

        for (var value : self.personality().values()) {
            sb.append(value.name()).append("(").append(String.format("%.2f", value.weight())).append(")、");
        }

        sb.append("我的能力：");
        for (var cap : self.capabilities()) {
            sb.append(cap.name()).append("、");
        }

        // 添加元认知信息
        if (self.metacognition() != null) {
            sb.append("自我认知能力：").append(self.metacognition().selfAwarenessLevel()).append("。");
        }

        return sb.toString();
    }

    /**
     * 获取主人画像（用于 LLM 上下文）
     */
    public String buildOwnerSummary() {
        WorldModel.World world = lastWorldModel;
        if (world == null || world.owner() == null) {
            return "";
        }

        OwnerModel.Owner owner = world.owner();
        StringBuilder sb = new StringBuilder();

        if (owner.identity() != null && owner.identity().name() != null) {
            sb.append("主人叫").append(owner.identity().name()).append("。");
        }

        if (owner.communicationStyle() != null) {
            sb.append("沟通风格：");
            sb.append(owner.communicationStyle().tone()).append("，");
            sb.append(owner.communicationStyle().verbosity()).append("。");
        }

        if (owner.workStyle() != null) {
            sb.append("工作风格：").append(owner.workStyle()).append("。");
        }

        // 信任等级
        if (owner.trustLevel() != null) {
            sb.append("信任等级：").append(String.format("%.2f", owner.trustLevel().level())).append("。");
        }

        // 当前情绪状态
        if (owner.emotionalState() != null) {
            sb.append("当前情绪：").append(owner.emotionalState().currentMood().name());
            sb.append("（强度：").append(String.format("%.2f", owner.emotionalState().intensity())).append("）。");
        }

        // 近期目标
        if (owner.goals() != null && !owner.goals().isEmpty()) {
            sb.append("近期目标：");
            owner.goals().stream().limit(3).forEach(g -> sb.append(g.description()).append("、"));
            sb.append("。");
        }

        // 习惯
        if (owner.habits() != null && !owner.habits().isEmpty()) {
            sb.append("已学习习惯：");
            owner.habits().stream().limit(3).forEach(h -> sb.append(h.pattern()).append("、"));
            sb.append("。");
        }

        return sb.toString();
    }

    /**
     * 获取记忆高亮（用于 LLM 上下文）
     */
    public String buildMemoryHighlights() {
        if (memory == null || memory.getWorking() == null) {
            return "（无相关记忆）";
        }

        StringBuilder sb = new StringBuilder();
        var items = memory.getWorking().getAll();
        if (!items.isEmpty()) {
            sb.append("工作记忆中的重要信息：");
            for (var item : items) {
                sb.append("- ").append(item.content()).append("\n");
            }
        }

        return sb.isEmpty() ? "（无相关记忆）" : sb.toString();
    }

    /**
     * 获取主人情绪状态
     */
    public String getOwnerMood() {
        if (lastWorldModel != null
                && lastWorldModel.owner() != null
                && lastWorldModel.owner().emotionalState() != null) {
            var emotionalState = lastWorldModel.owner().emotionalState();
            return emotionalState.currentMood().name() + " (强度: " + emotionalState.intensity() + ")";
        }
        return "UNKNOWN";
    }

    /**
     * 添加对话消息到历史记录
     */
    public void addChatMessage(String sessionId, String role, String content) {
        ChatMessage msg = new ChatMessage(role, content, Instant.now());
        chatHistory.put(sessionId + "-" + msg.timestamp().toEpochMilli(), msg);

        // 如果超过最大容量，压缩历史
        if (chatHistory.size() > MAX_HISTORY_SIZE) {
            compressHistory();
        }
    }

    /**
     * 压缩对话历史（保留最近一半）
     */
    private void compressHistory() {
        if (chatHistory.size() <= MAX_HISTORY_SIZE / 2) {
            return;
        }

        // 按时间排序，保留最近的一半
        List<ChatMessage> sorted = chatHistory.values().stream()
                .sorted((a, b) -> a.timestamp().compareTo(b.timestamp()))
                .toList();

        chatHistory.clear();
        int start = sorted.size() / 2;
        for (int i = start; i < sorted.size(); i++) {
            ChatMessage msg = sorted.get(i);
            chatHistory.put(sessionIdFromMsg(msg), msg);
        }

        logger.info("Chat history compressed from {} to {} messages", sorted.size(), chatHistory.size());
    }

    private String sessionIdFromMsg(ChatMessage msg) {
        return "session-" + msg.timestamp().toEpochMilli();
    }

    /**
     * 获取对话历史（用于构建上下文）
     */
    public String getChatHistory() {
        if (chatHistory.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        chatHistory.values().stream()
                .sorted((a, b) -> a.timestamp().compareTo(b.timestamp()))
                .forEach(msg -> {
                    sb.append(msg.role()).append(": ").append(msg.content()).append("\n");
                });

        return sb.toString();
    }
}
