package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 决策引擎 - 将推理结果转换为可执行的动作
 *
 * 核心职责：
 * 1. 接收 ReasoningOutput（意图/因果/预测）
 * 2. 基于 SelfModel 的价值观和规则权衡
 * 3. 生成可执行的 ToolCall 列表
 * 4. 支持基于上下文的动作优先级
 *
 * ## 决策策略
 *
 * - 紧急事务（电池低、任务紧迫）优先执行
 * - 基于意图类型选择对应动作
 * - 置信度加权：confidence 越高，动作越可靠
 * - 动作去重：避免重复执行相同动作
 */
public class DecisionEngine {

    // 意图类型 → 动作类型的映射规则
    private static final Map<ReasoningEngine.ReasoningType, String> INTENT_ACTION_MAP = new ConcurrentHashMap<>();
    // 动作优先级
    private static final Map<String, Integer> ACTION_PRIORITY = new ConcurrentHashMap<>();

    static {
        // 意图识别 → NotifyAction（通知主人）
        INTENT_ACTION_MAP.put(ReasoningEngine.ReasoningType.INTENT, "NotifyAction");

        // 因果推理 → LogAction（记录分析）
        INTENT_ACTION_MAP.put(ReasoningEngine.ReasoningType.CAUSAL, "LogAction");

        // 预测 → Remember（记住预测上下文）
        INTENT_ACTION_MAP.put(ReasoningEngine.ReasoningType.PREDICTION, "Remember");

        // 反思 → LogAction（记录反思）
        // INTENT_ACTION_MAP.put(ReasoningEngine.ReasoningType.REFLECTION, "LogAction");

        // 动作优先级（数字越大优先级越高）
        ACTION_PRIORITY.put("NotifyAction", 80);
        ACTION_PRIORITY.put("Remember", 60);
        ACTION_PRIORITY.put("LogAction", 40);
        ACTION_PRIORITY.put("Calculator", 70);
        ACTION_PRIORITY.put("SearchFiles", 65);
        ACTION_PRIORITY.put("RecallMemory", 55);
        ACTION_PRIORITY.put("Notify", 80);
        ACTION_PRIORITY.put("Search", 65);
    }

    private final WorldModel.World worldModel;

    public DecisionEngine(WorldModel.World worldModel) {
        this.worldModel = worldModel;
    }

    /**
     * 决策入口 - 将推理结果转换为可执行的动作
     *
     * @param reasoningResult 推理结果
     * @param salienceScore 显著性评分
     * @param selfModel 自我模型（用于价值观权衡）
     * @return 决策结果
     */
    public DecisionResult decide(
            ReasoningEngine.ReasoningResult reasoningResult,
            PerceptionSystem.SalienceScore salienceScore,
            SelfModel.Self selfModel
    ) {
        List<ToolCall> actions = new ArrayList<>();
        Set<String> executedActions = new HashSet<>();

        // 1. 基于显著性检测紧急事务
        if (salienceScore != null && salienceScore.overall() > 0.8) {
            String urgentAction = detectUrgentAction(salienceScore);
            if (urgentAction != null && !executedActions.contains(urgentAction)) {
                actions.add(new ToolCall(urgentAction, buildParams(urgentAction, "紧急关注: " + salienceScore)));
                executedActions.add(urgentAction);
            }
        }

        // 2. 基于意图推理生成动作
        if (reasoningResult != null && reasoningResult.hasLlmSupport()) {
            for (ReasoningEngine.ReasoningOutput output : reasoningResult.outputs()) {
                ToolCall action = convertToAction(output, selfModel);
                if (action != null && !executedActions.contains(action.tool())) {
                    actions.add(action);
                    executedActions.add(action.tool());
                }
            }
        }

        // 3. 基于情绪状态生成动作
        if (worldModel != null && worldModel.owner() != null
                && worldModel.owner().emotionalState() != null) {
            String emotionalAction = generateEmotionalAction(
                    worldModel.owner().emotionalState().currentMood(),
                    worldModel.owner().emotionalState().intensity()
            );
            if (emotionalAction != null && !executedActions.contains(emotionalAction)) {
                actions.add(new ToolCall(emotionalAction, buildParams(emotionalAction, "主人情绪状态")));
                executedActions.add(emotionalAction);
            }
        }

        // 4. 优先级排序
        actions.sort((a, b) -> {
            int priorityA = ACTION_PRIORITY.getOrDefault(a.tool(), 50);
            int priorityB = ACTION_PRIORITY.getOrDefault(b.tool(), 50);
            return Integer.compare(priorityB, priorityA); // 降序
        });

        // 5. 限制最大动作数
        if (actions.size() > 5) {
            actions = actions.subList(0, 5);
        }

        return new DecisionResult(actions, buildReason(actions, salienceScore, reasoningResult));
    }

    /**
     * 将推理输出转换为动作
     */
    private ToolCall convertToAction(ReasoningEngine.ReasoningOutput output, SelfModel.Self selfModel) {
        if (output == null || output.content() == null) {
            return null;
        }

        // 置信度过低时不生成动作
        if (output.confidence() < 0.5f) {
            return null;
        }

        String actionType = INTENT_ACTION_MAP.get(output.type());
        if (actionType == null) {
            return null;
        }

        // 根据动作类型构建参数
        Map<String, Object> params = buildParams(actionType, output.content());

        // 添加置信度到参数
        params.put("confidence", output.confidence());
        params.put("reasoningType", output.type().name());

        return new ToolCall(actionType, params);
    }

    /**
     * 检测紧急事务
     */
    private String detectUrgentAction(PerceptionSystem.SalienceScore salienceScore) {
        // 紧急标记检测
        if (salienceScore.urgency() > 0.8) {
            return "NotifyAction";
        }
        return null;
    }

    /**
     * 基于情绪生成动作
     */
    private String generateEmotionalAction(OwnerModel.Mood mood, float intensity) {
        if (intensity < 0.5) {
            return null; // 情绪平稳，不打扰
        }

        // 高强度情绪时发送通知
        return switch (mood) {
            case ANXIOUS, FRUSTRATED -> "NotifyAction"; // 主人情绪波动，主动关心
            default -> null;
        };
    }

    /**
     * 构建动作参数
     */
    private Map<String, Object> buildParams(String actionType, String content) {
        Map<String, Object> params = new ConcurrentHashMap<>();
        params.put("actionParam", content);
        params.put("timestamp", java.time.Instant.now());
        return params;
    }

    /**
     * 构建决策理由
     */
    private String buildReason(List<ToolCall> actions,
                               PerceptionSystem.SalienceScore salienceScore,
                               ReasoningEngine.ReasoningResult reasoningResult) {
        StringBuilder reason = new StringBuilder();

        if (salienceScore != null) {
            reason.append("显著性: ").append(String.format("%.2f", salienceScore.overall()));
        }

        if (reasoningResult != null && reasoningResult.hasLlmSupport()) {
            if (reason.length() > 0) reason.append(", ");
            reason.append("推理支持: ").append(reasoningResult.outputs().size()).append(" 条");
        }

        if (actions.size() > 0) {
            if (reason.length() > 0) reason.append(", ");
            reason.append("生成动作: ").append(actions.size()).append(" 个");
        }

        return reason.toString();
    }

    /**
     * 决策结果记录
     */
    public record DecisionResult(
            List<ToolCall> actions,
            String reason
    ) {
        public boolean hasActions() {
            return actions != null && !actions.isEmpty();
        }
    }

    /**
     * 可执行动作记录
     */
    public record ToolCall(
            String tool,
            Map<String, Object> params
    ) {}
}
