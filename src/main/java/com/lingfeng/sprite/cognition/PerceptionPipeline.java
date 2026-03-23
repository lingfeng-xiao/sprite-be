package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.PerceptionSystem;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S15-1: 感知管道 - 感知输入 → 注意力过滤
 *
 * ## 核心职责
 *
 * 1. **三重通道确认**：进程白名单 + 窗口变化 + 时间冷却
 * 2. **显著性评分**：计算综合显著性分数
 * 3. **过滤**：根据阈值过滤低显著性感知
 * 4. **学习型注意力**：基于历史模式预测显著性
 */
public class PerceptionPipeline {
    private final float significantThreshold;
    private final long cooldownSeconds;
    private final PerceptionSystem.AttentionMechanism attention;
    private final List<PerceptionSystem.SalienceScore> salienceHistory = new ArrayList<>();
    private PerceptionSystem.Perception lastPerception = null;

    // S15-1: 学习型注意力模型 - 历史模式追踪
    private final Map<String, PatternSalience> patternSalienceMap = new HashMap<>();
    private static final float EMA_ALPHA = 0.3f;  // 指数移动平均系数
    private static final int PATTERN_CACHE_SIZE = 50;

    public PerceptionPipeline() {
        this(0.5f, 300L);
    }

    public PerceptionPipeline(float significantThreshold, long cooldownSeconds) {
        this.significantThreshold = significantThreshold;
        this.cooldownSeconds = cooldownSeconds;
        this.attention = new PerceptionSystem.AttentionMechanism(3, cooldownSeconds);
    }

    /**
     * 处理感知输入，执行注意力过滤
     * S15-1: 同时学习历史模式以改进未来预测
     */
    public PipelineOutput process(PerceptionSystem.PerceptionResult perceptionResult) {
        PerceptionSystem.AttentionItem attentionItem = attention.process(
            perceptionResult.perception(),
            lastPerception
        );

        salienceHistory.add(attentionItem.salience());
        if (salienceHistory.size() > 100) {
            salienceHistory.remove(0);
        }

        // S15-1: 学习当前感知的显著性模式
        learnSalience(perceptionResult.perception(), attentionItem.salience().overall());

        boolean isSignificant = attentionItem.salience().overall() > significantThreshold;
        lastPerception = perceptionResult.perception();

        return new PipelineOutput(
            perceptionResult.perception(),
            attentionItem.salience(),
            isSignificant,
            attention.getChannelConfidence(),
            attention.getCurrentFocus()
        );
    }

    /**
     * 检查是否在冷却期
     */
    public boolean isOnCooldown(String actionType) {
        return attention.isOnCooldown(actionType);
    }

    /**
     * 记录动作（启动冷却）
     */
    public void recordAction(String actionType) {
        attention.recordAction(actionType);
    }

    /**
     * 获取平均显著性
     */
    public float getAverageSalience() {
        if (salienceHistory.isEmpty()) {
            return 0f;
        }
        double sum = 0;
        for (PerceptionSystem.SalienceScore s : salienceHistory) {
            sum += s.overall();
        }
        return (float) (sum / salienceHistory.size());
    }

    /**
     * 获取通道置信度
     */
    public Map<String, Float> getChannelConfidence() {
        return attention.getChannelConfidence();
    }

    public record PipelineOutput(
        PerceptionSystem.Perception filteredPerception,
        PerceptionSystem.SalienceScore salienceScore,
        boolean isSignificant,
        Map<String, Float> channelConfidence,
        List<PerceptionSystem.AttentionItem> currentFocus
    ) {}

    // S15-1: 学习型注意力模型 - 内部类
    /**
     * S15-1: 历史模式显著性记录
     */
    private static class PatternSalience {
        String patternKey;
        float emaSalience;  // 指数移动平均显著性
        int observationCount;
        Instant lastObserved;

        PatternSalience(String patternKey, float initialSalience) {
            this.patternKey = patternKey;
            this.emaSalience = initialSalience;
            this.observationCount = 1;
            this.lastObserved = Instant.now();
        }

        void update(float newSalience) {
            // 指数移动平均更新
            this.emaSalience = EMA_ALPHA * newSalience + (1 - EMA_ALPHA) * emaSalience;
            this.observationCount++;
            this.lastObserved = Instant.now();
        }
    }

    /**
     * S15-1: 生成刺激模式键
     * 基于环境上下文、时间、用户状态生成唯一模式标识
     */
    private String generatePatternKey(PerceptionSystem.Perception perception) {
        StringBuilder key = new StringBuilder();

        // 环境上下文
        if (perception.environment() != null) {
            key.append("ctx:").append(perception.environment().context()).append(";");
            key.append("hr:").append(perception.environment().hourOfDay()).append(";");
            key.append("dw:").append(perception.environment().dayOfWeek()).append(";");
        }

        // 用户状态
        if (perception.user() != null && perception.user().presence() != null) {
            key.append("prs:").append(perception.user().presence()).append(";");
        }

        // 活跃窗口类型
        if (perception.user() != null && perception.user().activeWindow() != null) {
            key.append("win:").append(perception.user().activeWindow().appType()).append(";");
        }

        return key.toString();
    }

    /**
     * S15-1: 基于历史模式预测显著性
     * @param perception 当前感知
     * @return 预测的显著性分数 (0-1)
     */
    public float predictSalience(PerceptionSystem.Perception perception) {
        String patternKey = generatePatternKey(perception);
        PatternSalience pattern = patternSalienceMap.get(patternKey);

        if (pattern == null) {
            // 无历史记录，返回中等基准
            return 0.5f;
        }

        // 根据历史模式预测，并考虑时间衰减
        long hoursSinceLastObserved = java.time.Duration.between(pattern.lastObserved, Instant.now()).toHours();
        float timeDecay = Math.max(0.5f, 1.0f - (hoursSinceLastObserved * 0.02f));

        // 观察次数越多，预测越可靠
        float reliabilityBoost = Math.min(1.2f, 1.0f + (pattern.observationCount * 0.02f));

        return Math.min(1.0f, pattern.emaSalience * timeDecay * reliabilityBoost);
    }

    /**
     * S15-1: 学习新观察到的显著性
     * @param perception 当前感知
     * @param actualSalience 实际显著性分数
     */
    public void learnSalience(PerceptionSystem.Perception perception, float actualSalience) {
        String patternKey = generatePatternKey(perception);

        PatternSalience existing = patternSalienceMap.get(patternKey);
        if (existing != null) {
            existing.update(actualSalience);
        } else {
            patternSalienceMap.put(patternKey, new PatternSalience(patternKey, actualSalience));

            // 限制缓存大小，防止内存溢出
            if (patternSalienceMap.size() > PATTERN_CACHE_SIZE) {
                pruneOldPatterns();
            }
        }
    }

    /**
     * S15-1: 清理旧模式，保留最近观察的模式
     */
    private void pruneOldPatterns() {
        Instant oldestAllowed = Instant.now().minusSeconds(86400 * 7); // 保留7天内的模式
        patternSalienceMap.entrySet().removeIf(entry ->
            entry.getValue().lastObserved.isBefore(oldestAllowed)
        );

        // 如果还是太大，删除最少使用的
        if (patternSalienceMap.size() > PATTERN_CACHE_SIZE) {
            patternSalienceMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(a.getValue().observationCount, b.getValue().observationCount))
                .limit(patternSalienceMap.size() - PATTERN_CACHE_SIZE / 2)
                .forEach(e -> patternSalienceMap.remove(e.getKey()));
        }
    }

    /**
     * S15-1: 获取模式预测的详细信息
     */
    public Map<String, Object> getPatternInsights() {
        Map<String, Object> insights = new HashMap<>();
        insights.put("totalPatterns", patternSalienceMap.size());
        insights.put("patterns", patternSalienceMap.entrySet().stream()
            .sorted((a, b) -> Float.compare(b.getValue().emaSalience, a.getValue().emaSalience))
            .limit(10)
            .map(e -> Map.of(
                "pattern", e.getKey(),
                "emaSalience", e.getValue().emaSalience,
                "observations", e.getValue().observationCount
            ))
            .toList());
        return insights;
    }

    /**
     * S15-1: 使用学习模型增强显著性评分
     * 将预测的显著性作为额外因子融入实际评分
     * @param baseSalience 基础显著性评分
     * @param perception 当前感知
     * @return 增强后的显著性评分
     */
    public PerceptionSystem.SalienceScore enhanceWithLearning(
            PerceptionSystem.SalienceScore baseSalience,
            PerceptionSystem.Perception perception) {
        float predictedSalience = predictSalience(perception);

        // 融合基础评分和学习预测 (加权平均)
        float learningWeight = 0.25f;
        float enhancedOverall = baseSalience.overall() * (1 - learningWeight)
                + predictedSalience * learningWeight;

        return new PerceptionSystem.SalienceScore(
            baseSalience.novelty(),
            baseSalience.relevance(),
            baseSalience.urgency(),
            baseSalience.emotional(),
            enhancedOverall
        );
    }
}
