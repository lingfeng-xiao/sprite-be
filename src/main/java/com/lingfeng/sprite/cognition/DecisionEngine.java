package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;

import java.util.*;
import java.time.Instant;
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
 *
 * ## S6-1 多维度决策
 *
 * 决策综合考虑以下维度：
 * - 显著性维度（novelty, relevance, urgency）
 * - 情绪维度（主人情绪状态和强度）
 * - 时间维度（当前时间上下文）
 * - 记忆维度（相关记忆检索结果）
 * - 偏好维度（主人交互偏好）
 */
public class DecisionEngine {

    /**
     * S6-1: 决策维度枚举
     */
    public enum DecisionDimension {
        NOVELTY("显著性-新颖性", 0.15f),
        RELEVANCE("显著性-相关性", 0.20f),
        URGENCY("显著性-紧急性", 0.25f),
        EMOTION("情绪维度", 0.15f),
        TIME_CONTEXT("时间上下文", 0.10f),
        MEMORY("记忆维度", 0.10f),
        PREFERENCE("偏好维度", 0.05f);

        private final String description;
        private final float defaultWeight;

        DecisionDimension(String description, float defaultWeight) {
            this.description = description;
            this.defaultWeight = defaultWeight;
        }

        public String description() { return description; }
        public float defaultWeight() { return defaultWeight; }
    }

    /**
     * S6-1: 单个维度的评分结果
     */
    public record DimensionScore(
            DecisionDimension dimension,
            float rawScore,
            float weightedScore
    ) {}

    /**
     * S6-1: 多维度评估结果
     */
    public record MultiDimensionalEvaluation(
            float overallScore,
            List<DimensionScore> dimensionScores,
            String reasoning
    ) {}

    /**
     * S6-2: 置信度等级枚举
     */
    public enum ConfidenceLevel {
        HIGH("高置信度", 0.8f),
        MEDIUM("中置信度", 0.5f),
        LOW("低置信度", 0.3f),
        UNKNOWN("未知置信度", 0.0f);

        private final String description;
        private final float threshold;

        ConfidenceLevel(String description, float threshold) {
            this.description = description;
            this.threshold = threshold;
        }

        public String description() { return description; }
        public float threshold() { return threshold; }

        public static ConfidenceLevel fromScore(float score) {
            if (score >= 0.8f) return HIGH;
            if (score >= 0.5f) return MEDIUM;
            if (score >= 0.3f) return LOW;
            return UNKNOWN;
        }
    }

    /**
     * S6-2: 置信度摘要
     */
    public record ConfidenceSummary(
            float overallConfidence,
            ConfidenceLevel level,
            float reasoningConfidence,     // 推理置信度
            float memoryConfidence,        // 记忆置信度
            float emotionConfidence,       // 情绪置信度
            float habitConfidence,         // 习惯置信度
            String reasoning
    ) {}

    /**
     * S6-2: 决策来源置信度
     */
    public record SourceConfidence(
            String source,
            float confidence,
            ConfidenceLevel level,
            String explanation
    ) {}

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

    // S6-1: 维度权重配置
    private final Map<DecisionDimension, Float> dimensionWeights = new ConcurrentHashMap<>();

    // S6-2: 置信度阈值配置
    private float highConfidenceThreshold = 0.8f;
    private float mediumConfidenceThreshold = 0.5f;
    private float lowConfidenceThreshold = 0.3f;

    // S6-3: 决策历史存储
    private final List<DecisionHistory> decisionHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100; // 最多保留100条决策记录

    public DecisionEngine(WorldModel.World worldModel) {
        this.worldModel = worldModel;
        // 初始化默认权重
        for (DecisionDimension dim : DecisionDimension.values()) {
            dimensionWeights.put(dim, dim.defaultWeight());
        }
    }

    /**
     * S6-1: 获取维度权重
     */
    public Map<DecisionDimension, Float> getDimensionWeights() {
        return new EnumMap<>(dimensionWeights);
    }

    /**
     * S6-1: 设置维度权重
     */
    public void setDimensionWeight(DecisionDimension dimension, float weight) {
        if (weight >= 0 && weight <= 1) {
            dimensionWeights.put(dimension, weight);
        }
    }

    /**
     * S6-1: 重置维度权重到默认值
     */
    public void resetDimensionWeights() {
        for (DecisionDimension dim : DecisionDimension.values()) {
            dimensionWeights.put(dim, dim.defaultWeight());
        }
    }

    /**
     * S6-2: 获取置信度阈值
     */
    public float[] getConfidenceThresholds() {
        return new float[]{highConfidenceThreshold, mediumConfidenceThreshold, lowConfidenceThreshold};
    }

    /**
     * S6-2: 设置置信度阈值
     */
    public void setConfidenceThresholds(float high, float medium, float low) {
        if (high >= medium && medium >= low) {
            this.highConfidenceThreshold = high;
            this.mediumConfidenceThreshold = medium;
            this.lowConfidenceThreshold = low;
        }
    }

    /**
     * S6-2: 计算置信度摘要
     */
    public ConfidenceSummary calculateConfidenceSummary(
            ReasoningEngine.ReasoningResult reasoningResult,
            MemoryRetrievalService.RetrievalContext retrievalContext,
            MultiDimensionalEvaluation evaluation
    ) {
        // 1. 计算推理置信度
        float reasoningConfidence = calculateReasoningConfidence(reasoningResult);

        // 2. 计算记忆置信度
        float memoryConfidence = calculateMemoryConfidence(retrievalContext);

        // 3. 计算情绪置信度
        float emotionConfidence = calculateEmotionConfidence();

        // 4. 计算习惯置信度
        float habitConfidence = calculateHabitConfidence(retrievalContext);

        // 5. 综合计算整体置信度（加权平均）
        float overallConfidence =
                reasoningConfidence * 0.35f +
                memoryConfidence * 0.30f +
                emotionConfidence * 0.20f +
                habitConfidence * 0.15f;

        // 6. 确定置信度等级
        ConfidenceLevel level;
        if (overallConfidence >= highConfidenceThreshold) {
            level = ConfidenceLevel.HIGH;
        } else if (overallConfidence >= mediumConfidenceThreshold) {
            level = ConfidenceLevel.MEDIUM;
        } else if (overallConfidence >= lowConfidenceThreshold) {
            level = ConfidenceLevel.LOW;
        } else {
            level = ConfidenceLevel.UNKNOWN;
        }

        String reasoning = String.format(
                "推理=%.2f, 记忆=%.2f, 情绪=%.2f, 习惯=%.2f, 综合=%.2f(%s)",
                reasoningConfidence, memoryConfidence, emotionConfidence, habitConfidence,
                overallConfidence, level.description()
        );

        return new ConfidenceSummary(
                overallConfidence,
                level,
                reasoningConfidence,
                memoryConfidence,
                emotionConfidence,
                habitConfidence,
                reasoning
        );
    }

    /**
     * S6-2: 计算推理置信度
     */
    private float calculateReasoningConfidence(ReasoningEngine.ReasoningResult reasoningResult) {
        if (reasoningResult == null || !reasoningResult.hasLlmSupport()) {
            return 0.0f;
        }

        float totalConfidence = 0f;
        int count = 0;

        for (ReasoningEngine.ReasoningOutput output : reasoningResult.outputs()) {
            totalConfidence += output.confidence();
            count++;
        }

        return count > 0 ? totalConfidence / count : 0.0f;
    }

    /**
     * S6-2: 计算记忆置信度
     */
    private float calculateMemoryConfidence(MemoryRetrievalService.RetrievalContext retrievalContext) {
        if (retrievalContext == null || retrievalContext.isEmpty()) {
            return 0.0f;
        }

        // 记忆检索的总体相关性作为置信度
        return retrievalContext.overallRelevance();
    }

    /**
     * S6-2: 计算情绪置信度
     */
    private float calculateEmotionConfidence() {
        if (worldModel == null || worldModel.owner() == null
                || worldModel.owner().emotionalState() == null) {
            return 0.0f;
        }

        // 情绪置信度基于情绪强度和一致性
        float intensity = worldModel.owner().emotionalState().intensity();
        return Math.min(1.0f, intensity * 1.2f); // 强度高的情绪更可信
    }

    /**
     * S6-2: 计算习惯置信度
     */
    private float calculateHabitConfidence(MemoryRetrievalService.RetrievalContext retrievalContext) {
        if (retrievalContext == null || retrievalContext.isEmpty()) {
            return 0.0f;
        }

        // 如果检索到感知模式（习惯），给予较高置信度
        if (!retrievalContext.relevantPatterns().isEmpty()) {
            return 0.7f + (retrievalContext.overallRelevance() * 0.3f);
        }

        return 0.0f;
    }

    /**
     * S6-2: 置信度传播算法 - 将子维度置信度传播到父维度
     */
    public float propagateConfidence(float childConfidence, float parentWeight) {
        // 简单的加权传播
        return childConfidence * parentWeight;
    }

    /**
     * S6-1: 多维度评估 - 对给定上下文进行综合多维度评分
     *
     * @param salienceScore 显著性评分
     * @param retrievalContext 记忆检索上下文
     * @return 多维度评估结果
     */
    public MultiDimensionalEvaluation evaluateMultiDimensional(
            PerceptionSystem.SalienceScore salienceScore,
            MemoryRetrievalService.RetrievalContext retrievalContext
    ) {
        List<DimensionScore> scores = new ArrayList<>();
        float totalWeightedScore = 0f;
        StringBuilder reasoning = new StringBuilder();

        // 1. 计算显著性维度评分
        if (salienceScore != null) {
            float noveltyRaw = salienceScore.novelty();
            float noveltyWeighted = noveltyRaw * dimensionWeights.get(DecisionDimension.NOVELTY);
            scores.add(new DimensionScore(DecisionDimension.NOVELTY, noveltyRaw, noveltyWeighted));
            totalWeightedScore += noveltyWeighted;

            float relevanceRaw = salienceScore.relevance();
            float relevanceWeighted = relevanceRaw * dimensionWeights.get(DecisionDimension.RELEVANCE);
            scores.add(new DimensionScore(DecisionDimension.RELEVANCE, relevanceRaw, relevanceWeighted));
            totalWeightedScore += relevanceWeighted;

            float urgencyRaw = salienceScore.urgency();
            float urgencyWeighted = urgencyRaw * dimensionWeights.get(DecisionDimension.URGENCY);
            scores.add(new DimensionScore(DecisionDimension.URGENCY, urgencyRaw, urgencyWeighted));
            totalWeightedScore += urgencyWeighted;

            reasoning.append(String.format("显著性: 新颖性=%.2f, 相关性=%.2f, 紧急性=%.2f; ",
                    noveltyRaw, relevanceRaw, urgencyRaw));
        }

        // 2. 计算情绪维度评分
        float emotionRaw = 0f;
        if (worldModel != null && worldModel.owner() != null
                && worldModel.owner().emotionalState() != null) {
            float intensity = worldModel.owner().emotionalState().intensity();
            OwnerModel.Mood mood = worldModel.owner().emotionalState().currentMood();
            // 情绪强度作为情绪维度评分，高强度情绪需要关注
            emotionRaw = intensity;
            // 负面情绪提高评分（需要更多关注）
            if (mood == OwnerModel.Mood.ANXIOUS || mood == OwnerModel.Mood.FRUSTRATED) {
                emotionRaw = Math.min(1.0f, emotionRaw * 1.2f);
            }
        }
        float emotionWeighted = emotionRaw * dimensionWeights.get(DecisionDimension.EMOTION);
        scores.add(new DimensionScore(DecisionDimension.EMOTION, emotionRaw, emotionWeighted));
        totalWeightedScore += emotionWeighted;
        reasoning.append(String.format("情绪=%.2f; ", emotionRaw));

        // 3. 计算时间上下文维度评分
        float timeRaw = evaluateTimeContext();
        float timeWeighted = timeRaw * dimensionWeights.get(DecisionDimension.TIME_CONTEXT);
        scores.add(new DimensionScore(DecisionDimension.TIME_CONTEXT, timeRaw, timeWeighted));
        totalWeightedScore += timeWeighted;
        reasoning.append(String.format("时间上下文=%.2f; ", timeRaw));

        // 4. 计算记忆维度评分
        float memoryRaw = 0f;
        if (retrievalContext != null && !retrievalContext.isEmpty()) {
            memoryRaw = retrievalContext.overallRelevance();
        }
        float memoryWeighted = memoryRaw * dimensionWeights.get(DecisionDimension.MEMORY);
        scores.add(new DimensionScore(DecisionDimension.MEMORY, memoryRaw, memoryWeighted));
        totalWeightedScore += memoryWeighted;
        reasoning.append(String.format("记忆相关性=%.2f; ", memoryRaw));

        // 5. 计算偏好维度评分
        float preferenceRaw = evaluatePreference();
        float preferenceWeighted = preferenceRaw * dimensionWeights.get(DecisionDimension.PREFERENCE);
        scores.add(new DimensionScore(DecisionDimension.PREFERENCE, preferenceRaw, preferenceWeighted));
        totalWeightedScore += preferenceWeighted;
        reasoning.append(String.format("偏好匹配=%.2f; ", preferenceRaw));

        return new MultiDimensionalEvaluation(
                totalWeightedScore,
                scores,
                reasoning.toString()
        );
    }

    /**
     * S6-1: 评估时间上下文维度
     * 根据当前时间判断是否是联系主人的好时机
     */
    private float evaluateTimeContext() {
        if (worldModel == null || worldModel.owner() == null) {
            return 0.5f; // 默认中等分数
        }

        // 获取当前时间上下文
        var envContext = worldModel.owner().context().environment();
        if (envContext == null) {
            return 0.5f;
        }

        var contextType = envContext.context();
        if (contextType == null) {
            return 0.5f;
        }

        // 根据上下文类型评估时间是否合适
        return switch (contextType) {
            case WORK -> 0.8f;      // 工作时间可能需要关注
            case MEETING -> 0.3f;    // 会议中不合适打扰
            case COMMUTE -> 0.5f;    // 通勤时间看情况
            case LEISURE -> 0.9f;    // 休闲时间是好的联系时机
            case RITUAL -> 0.7f;     // 晨间习惯时间可以
            case SLEEP -> 0.1f;      // 睡眠时间不合适
            default -> 0.5f;
        };
    }

    /**
     * S6-1: 评估偏好维度
     * 根据主人的交互偏好评估当前情况是否合适
     */
    private float evaluatePreference() {
        if (worldModel == null || worldModel.owner() == null) {
            return 0.5f;
        }

        // 简化实现：基于主人的联系偏好
        // 实际应该从 InteractionPreferenceLearningService 获取偏好数据
        float baseScore = 0.5f;

        // 检查主人情绪
        if (worldModel.owner().emotionalState() != null) {
            OwnerModel.Mood mood = worldModel.owner().emotionalState().currentMood();
            float intensity = worldModel.owner().emotionalState().intensity();

            // 主人情绪不好时减少打扰
            if (mood == OwnerModel.Mood.ANXIOUS || mood == OwnerModel.Mood.FRUSTRATED) {
                if (intensity > 0.7f) {
                    baseScore = 0.3f;
                }
            }
            // 主人情绪好时可以适当增加联系
            else if (mood == OwnerModel.Mood.HAPPY || mood == OwnerModel.Mood.EXCITED) {
                if (intensity > 0.6f) {
                    baseScore = 0.8f;
                }
            }
        }

        return baseScore;
    }

    /**
     * 决策入口 - 将推理结果转换为可执行的动作
     *
     * @param reasoningResult 推理结果
     * @param salienceScore 显著性评分
     * @param selfModel 自我模型（用于价值观权衡）
     * @param retrievalContext 记忆检索上下文（可选）
     * @return 决策结果
     */
    public DecisionResult decide(
            ReasoningEngine.ReasoningResult reasoningResult,
            PerceptionSystem.SalienceScore salienceScore,
            SelfModel.Self selfModel,
            MemoryRetrievalService.RetrievalContext retrievalContext
    ) {
        List<ToolCall> actions = new ArrayList<>();
        Set<String> executedActions = new HashSet<>();

        // 0. 基于记忆检索结果生成回忆触发动作
        if (retrievalContext != null && !retrievalContext.isEmpty()) {
            List<ToolCall> recallActions = generateRecallTriggeredActions(retrievalContext);
            for (ToolCall action : recallActions) {
                if (!executedActions.contains(action.tool())) {
                    actions.add(action);
                    executedActions.add(action.tool());
                }
            }
        }

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

        // S6-1: 计算多维度评估结果
        MultiDimensionalEvaluation evaluation = evaluateMultiDimensional(salienceScore, retrievalContext);

        // S6-2: 计算置信度摘要
        ConfidenceSummary confidence = calculateConfidenceSummary(reasoningResult, retrievalContext, evaluation);

        // S6-3: 构建决策结果
        DecisionResult result = new DecisionResult(actions, buildReason(actions, salienceScore, reasoningResult, retrievalContext), evaluation, confidence);

        // S6-3: 记录决策到历史
        recordDecision(result);

        return result;
    }

    /**
     * 兼容旧版本的 decide 方法
     */
    public DecisionResult decide(
            ReasoningEngine.ReasoningResult reasoningResult,
            PerceptionSystem.SalienceScore salienceScore,
            SelfModel.Self selfModel
    ) {
        return decide(reasoningResult, salienceScore, selfModel, null);
    }

    /**
     * 基于检索到的记忆生成触发动作
     */
    private List<ToolCall> generateRecallTriggeredActions(MemoryRetrievalService.RetrievalContext context) {
        List<ToolCall> actions = new ArrayList<>();

        // 1. 如果检索到强烈的情感记忆（正面经验），触发情绪提振
        if (context.overallRelevance() > 0.5f && !context.relevantEpisodic().isEmpty()) {
            // 检查是否有类似情绪的成功经验
            for (String episodic : context.relevantEpisodic()) {
                if (episodic.contains("【类似情绪】")) {
                    // 找到类似情绪的正面经验，可以用于情绪提振
                    Map<String, Object> params = new ConcurrentHashMap<>();
                    params.put("actionParam", "主人之前在类似情绪下有成功经验：" + episodic);
                    params.put("timestamp", java.time.Instant.now());
                    params.put("memoryType", "positive_recall");
                    actions.add(new ToolCall("NotifyAction", params));
                    break;
                }
            }
        }

        // 2. 如果检索到程序记忆（技能），增强相关决策置信度
        if (!context.relevantProcedural().isEmpty()) {
            Map<String, Object> params = new ConcurrentHashMap<>();
            params.put("actionParam", "相关技能记忆：" + String.join(", ", context.relevantProcedural()));
            params.put("timestamp", java.time.Instant.now());
            params.put("memoryType", "skill_recall");
            actions.add(new ToolCall("Remember", params));
        }

        // 3. 如果检索到感知模式（习惯），预判主人需求
        if (!context.relevantPatterns().isEmpty()) {
            Map<String, Object> params = new ConcurrentHashMap<>();
            params.put("actionParam", "检测到习惯模式：" + String.join(", ", context.relevantPatterns()));
            params.put("timestamp", java.time.Instant.now());
            params.put("memoryType", "pattern_recall");
            actions.add(new ToolCall("Remember", params));
        }

        return actions;
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
                               ReasoningEngine.ReasoningResult reasoningResult,
                               MemoryRetrievalService.RetrievalContext retrievalContext) {
        StringBuilder reason = new StringBuilder();

        if (salienceScore != null) {
            reason.append("显著性: ").append(String.format("%.2f", salienceScore.overall()));
        }

        if (reasoningResult != null && reasoningResult.hasLlmSupport()) {
            if (reason.length() > 0) reason.append(", ");
            reason.append("推理支持: ").append(reasoningResult.outputs().size()).append(" 条");
        }

        if (retrievalContext != null && !retrievalContext.isEmpty()) {
            if (reason.length() > 0) reason.append(", ");
            reason.append("记忆检索: ").append(String.format("%.0f%%", retrievalContext.overallRelevance() * 100));
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
            String reason,
            MultiDimensionalEvaluation evaluation,
            ConfidenceSummary confidence
    ) {
        public boolean hasActions() {
            return actions != null && !actions.isEmpty();
        }

        /**
         * 获取决策整体评分
         */
        public float getOverallScore() {
            return evaluation != null ? evaluation.overallScore() : 0f;
        }

        /**
         * 获取决策置信度
         */
        public float getConfidence() {
            return confidence != null ? confidence.overallConfidence() : 0f;
        }
    }

    /**
     * S6-3: 决策历史记录
     */
    public record DecisionHistory(
            Instant timestamp,
            List<ToolCall> actions,
            String reason,
            MultiDimensionalEvaluation evaluation,
            ConfidenceSummary confidence,
            int actionCount
    ) {}

    /**
     * S6-3: 决策统计
     */
    public record DecisionStatistics(
            int totalDecisions,
            int totalActions,
            Map<String, Integer> actionTypeCounts,  // 各动作类型出现次数
            float averageConfidence,
            float averageOverallScore,
            Instant lastDecisionTime,
            Map<String, Float> dimensionAverageScores
    ) {}

    /**
     * S6-3: 决策历史摘要
     */
    public record DecisionHistorySummary(
            List<DecisionHistory> recentDecisions,
            DecisionStatistics statistics,
            String timeRange
    ) {}

    /**
     * S6-3: 记录一条决策到历史
     */
    public void recordDecision(DecisionResult result) {
        DecisionHistory history = new DecisionHistory(
                Instant.now(),
                result.actions(),
                result.reason(),
                result.evaluation(),
                result.confidence(),
                result.actions() != null ? result.actions().size() : 0
        );
        decisionHistory.add(history);

        // 保持历史记录在限制范围内
        while (decisionHistory.size() > MAX_HISTORY_SIZE) {
            decisionHistory.remove(0);
        }
    }

    /**
     * S6-3: 获取决策历史
     */
    public List<DecisionHistory> getDecisionHistory() {
        return new ArrayList<>(decisionHistory);
    }

    /**
     * S6-3: 获取最近的N条决策历史
     */
    public List<DecisionHistory> getRecentDecisions(int count) {
        int size = Math.min(count, decisionHistory.size());
        return new ArrayList<>(decisionHistory.subList(decisionHistory.size() - size, decisionHistory.size()));
    }

    /**
     * S6-3: 获取决策统计
     */
    public DecisionStatistics getDecisionStatistics() {
        if (decisionHistory.isEmpty()) {
            return new DecisionStatistics(
                    0, 0,
                    Map.of(),
                    0f, 0f,
                    null,
                    Map.of()
            );
        }

        int totalDecisions = decisionHistory.size();
        int totalActions = 0;
        Map<String, Integer> actionTypeCounts = new ConcurrentHashMap<>();
        float totalConfidence = 0f;
        float totalScore = 0f;
        Map<DecisionDimension, Float> dimensionTotals = new EnumMap<>(DecisionDimension.class);

        for (DecisionDimension dim : DecisionDimension.values()) {
            dimensionTotals.put(dim, 0f);
        }

        for (DecisionHistory history : decisionHistory) {
            totalActions += history.actionCount();

            // 统计动作类型
            if (history.actions() != null) {
                for (ToolCall action : history.actions()) {
                    actionTypeCounts.merge(action.tool(), 1, Integer::sum);
                }
            }

            // 累计置信度
            if (history.confidence() != null) {
                totalConfidence += history.confidence().overallConfidence();
            }

            // 累计评分
            if (history.evaluation() != null) {
                totalScore += history.evaluation().overallScore();

                // 累计各维度评分
                for (DimensionScore ds : history.evaluation().dimensionScores()) {
                    dimensionTotals.merge(ds.dimension(), ds.weightedScore(), Float::sum);
                }
            }
        }

        // 计算各维度平均值
        Map<String, Float> dimensionAverages = new HashMap<>();
        for (Map.Entry<DecisionDimension, Float> entry : dimensionTotals.entrySet()) {
            dimensionAverages.put(entry.getKey().name(), entry.getValue() / totalDecisions);
        }

        return new DecisionStatistics(
                totalDecisions,
                totalActions,
                actionTypeCounts,
                totalConfidence / totalDecisions,
                totalScore / totalDecisions,
                decisionHistory.get(decisionHistory.size() - 1).timestamp(),
                dimensionAverages
        );
    }

    /**
     * S6-3: 获取指定时间范围内的决策历史
     */
    public List<DecisionHistory> getDecisionHistorySince(Instant since) {
        return decisionHistory.stream()
                .filter(h -> h.timestamp().isAfter(since))
                .toList();
    }

    /**
     * S6-3: 获取决策历史摘要
     */
    public DecisionHistorySummary getDecisionHistorySummary(int recentCount) {
        List<DecisionHistory> recent = getRecentDecisions(recentCount);
        DecisionStatistics stats = getDecisionStatistics();

        String timeRange = recent.isEmpty() ? "无历史记录" :
                String.format("%s 至 %s",
                        recent.get(0).timestamp(),
                        recent.get(recent.size() - 1).timestamp());

        return new DecisionHistorySummary(recent, stats, timeRange);
    }

    /**
     * S6-3: 清空决策历史
     */
    public void clearDecisionHistory() {
        decisionHistory.clear();
    }

    /**
     * 可执行动作记录
     */
    public record ToolCall(
            String tool,
            Map<String, Object> params
    ) {}
}
