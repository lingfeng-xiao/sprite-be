package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.PerceptionSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 感知管道 - 感知输入 → 注意力过滤
 *
 * ## 核心职责
 *
 * 1. **三重通道确认**：进程白名单 + 窗口变化 + 时间冷却
 * 2. **显著性评分**：计算综合显著性分数
 * 3. **过滤**：根据阈值过滤低显著性感知
 */
public class PerceptionPipeline {
    private final float significantThreshold;
    private final long cooldownSeconds;
    private final PerceptionSystem.AttentionMechanism attention;
    private final List<PerceptionSystem.SalienceScore> salienceHistory = new ArrayList<>();
    private PerceptionSystem.Perception lastPerception = null;

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
}
