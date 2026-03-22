package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.MemorySystem;
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

    public record ConversationFeedback(
            String sessionId,
            String userMessage,
            String assistantResponse,
            boolean success,
            Instant timestamp
    ) {}

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
        sb.append("我的价值观：");

        for (var value : self.values()) {
            sb.append(value.name()).append("(").append(value.weight()).append(")、");
        }

        sb.append("我的能力：");
        for (var cap : self.capabilities()) {
            sb.append(cap.name()).append("、");
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

        WorldModel.Owner owner = world.owner();
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
}
