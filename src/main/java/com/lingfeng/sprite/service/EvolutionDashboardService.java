package com.lingfeng.sprite.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.EvolutionEngine;

/**
 * S10-3: 进化历史Dashboard服务
 *
 * 提供进化历史的可视化数据：
 * - 进化趋势图
 * - 洞察历史
 * - 行为改变追踪
 * - 学习速率历史
 */
public class EvolutionDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(EvolutionDashboardService.class);

    private final List<EvolutionSnapshot> evolutionHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 100;

    /**
     * 进化快照
     */
    public record EvolutionSnapshot(
        Instant timestamp,
        int evolutionLevel,
        int totalEvolutions,
        float globalLearningRate,
        int insightsCount,
        int principlesCount,
        int behaviorsCount,
        int modificationsCount
    ) {}

    /**
     * 进化趋势数据
     */
    public record EvolutionTrend(
        List<Integer> evolutionLevels,
        List<Float> learningRates,
        List<Integer> insightCounts,
        List<Integer> principleCounts
    ) {}

    /**
     * 进化Dashboard数据
     */
    public record EvolutionDashboardData(
        Instant timestamp,
        int currentLevel,
        int totalEvolutions,
        EvolutionTrend trend,
        List<EvolutionSnapshot> recentHistory,
        InsightSummary insightSummary,
        BehaviorSummary behaviorSummary
    ) {}

    /**
     * 洞察摘要
     */
    public record InsightSummary(
        int totalInsights,
        int recentInsights,
        float averageConfidence,
        String mostCommonType
    ) {}

    /**
     * 行为摘要
     */
    public record BehaviorSummary(
        int totalChanges,
        int successfulChanges,
        float successRate,
        List<String> recentBehaviorPatterns
    ) {}

    public EvolutionDashboardService() {}

    // ==================== 快照管理 ====================

    /**
     * 记录进化快照
     */
    public void recordSnapshot(EvolutionEngine engine) {
        try {
            EvolutionEngine.EvolutionStatus status = engine.getStatus();

            EvolutionSnapshot snapshot = new EvolutionSnapshot(
                Instant.now(),
                status.evolutionLevel(),
                status.evolutionCount(),
                status.learningStats().globalLearningRate(),
                status.learningStats().totalInsights(),
                status.learningStats().totalPrinciples(),
                status.learningStats().totalBehaviorChanges(),
                status.recentModifications().size()
            );

            evolutionHistory.add(snapshot);

            // 清理旧快照
            while (evolutionHistory.size() > MAX_HISTORY) {
                evolutionHistory.remove(0);
            }

            logger.debug("Recorded evolution snapshot: level={}, count={}",
                snapshot.evolutionLevel(), snapshot.totalEvolutions());
        } catch (Exception e) {
            logger.error("Failed to record evolution snapshot: {}", e.getMessage());
        }
    }

    /**
     * 记录快照（从可视化数据）
     */
    public void recordSnapshot(EvolutionEngine.EvolutionHistoryVisualization visualization) {
        EvolutionSnapshot snapshot = new EvolutionSnapshot(
            visualization.timestamp(),
            visualization.evolutionLevel(),
            visualization.totalEvolutions(),
            visualization.learningRates().globalLearningRate(),
            visualization.insights().totalCount(),
            visualization.principles().totalCount(),
            visualization.behaviors().totalCount(),
            visualization.modifications().totalCount()
        );

        evolutionHistory.add(snapshot);

        while (evolutionHistory.size() > MAX_HISTORY) {
            evolutionHistory.remove(0);
        }
    }

    // ==================== 数据获取 ====================

    /**
     * 获取进化Dashboard数据
     */
    public EvolutionDashboardData getDashboardData(EvolutionEngine engine) {
        Instant now = Instant.now();
        Instant oneDayAgo = now.minus(24, ChronoUnit.HOURS);

        // 获取趋势数据
        List<Integer> levels = new ArrayList<>();
        List<Float> rates = new ArrayList<>();
        List<Integer> insightCounts = new ArrayList<>();
        List<Integer> principleCounts = new ArrayList<>();

        for (EvolutionSnapshot snapshot : evolutionHistory) {
            levels.add(snapshot.evolutionLevel());
            rates.add(snapshot.globalLearningRate());
            insightCounts.add(snapshot.insightsCount());
            principleCounts.add(snapshot.principlesCount());
        }

        EvolutionTrend trend = new EvolutionTrend(levels, rates, insightCounts, principleCounts);

        // 获取最近快照
        List<EvolutionSnapshot> recentHistory = evolutionHistory.size() > 10 ?
            evolutionHistory.subList(evolutionHistory.size() - 10, evolutionHistory.size()) :
            new ArrayList<>(evolutionHistory);

        // 获取洞察摘要
        int totalInsights = 0;
        int recentInsights = 0;
        float avgConfidence = 0f;
        String mostCommonType = "N/A";

        try {
            EvolutionEngine.EvolutionHistoryVisualization viz = engine.getEvolutionVisualization();
            totalInsights = viz.insights().totalCount();
            recentInsights = viz.insights().recentInsights().size();
            avgConfidence = viz.insights().confidenceTrend().averageConfidence();
            recentInsights = viz.insights().recentInsights().size();
        } catch (Exception e) {
            // 忽略
        }

        InsightSummary insightSummary = new InsightSummary(
            totalInsights, recentInsights, avgConfidence, mostCommonType
        );

        // 获取行为摘要
        int totalBehaviors = 0;
        int successfulBehaviors = 0;
        float behaviorSuccessRate = 0f;
        List<String> recentPatterns = List.of();

        try {
            EvolutionEngine.EvolutionHistoryVisualization viz = engine.getEvolutionVisualization();
            totalBehaviors = viz.behaviors().totalCount();
            successfulBehaviors = viz.behaviors().successfulChanges();
            behaviorSuccessRate = totalBehaviors > 0 ?
                (successfulBehaviors * 100f / totalBehaviors) : 0;
            recentPatterns = viz.behaviors().behaviorPatterns();
        } catch (Exception e) {
            // 忽略
        }

        BehaviorSummary behaviorSummary = new BehaviorSummary(
            totalBehaviors, successfulBehaviors, behaviorSuccessRate, recentPatterns
        );

        // 当前级别
        int currentLevel = 1;
        int totalEvolutions = evolutionHistory.size();
        if (!evolutionHistory.isEmpty()) {
            EvolutionSnapshot last = evolutionHistory.get(evolutionHistory.size() - 1);
            currentLevel = last.evolutionLevel();
            totalEvolutions = last.totalEvolutions();
        }

        return new EvolutionDashboardData(
            now,
            currentLevel,
            totalEvolutions,
            trend,
            recentHistory,
            insightSummary,
            behaviorSummary
        );
    }

    /**
     * 获取进化历史
     */
    public List<EvolutionSnapshot> getHistory() {
        return new ArrayList<>(evolutionHistory);
    }

    /**
     * 获取指定天数的历史
     */
    public List<EvolutionSnapshot> getHistory(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return evolutionHistory.stream()
            .filter(s -> s.timestamp().isAfter(cutoff))
            .toList();
    }

    // ==================== 趋势分析 ====================

    /**
     * 分析进化趋势
     */
    public String analyzeTrend() {
        if (evolutionHistory.size() < 2) {
            return "Not enough data for trend analysis";
        }

        int firstLevel = evolutionHistory.get(0).evolutionLevel();
        int lastLevel = evolutionHistory.get(evolutionHistory.size() - 1).evolutionLevel();

        float firstRate = evolutionHistory.get(0).globalLearningRate();
        float lastRate = evolutionHistory.get(evolutionHistory.size() - 1).globalLearningRate();

        StringBuilder sb = new StringBuilder();
        sb.append("Evolution Trend Analysis:\n");

        if (lastLevel > firstLevel) {
            sb.append(String.format("  - Level increased: %d -> %d\n", firstLevel, lastLevel));
        } else if (lastLevel < firstLevel) {
            sb.append(String.format("  - Level decreased: %d -> %d\n", firstLevel, lastLevel));
        } else {
            sb.append("  - Level unchanged\n");
        }

        if (lastRate > firstRate * 1.1f) {
            sb.append(String.format("  - Learning rate INCREASING: %.2f -> %.2f\n", firstRate, lastRate));
        } else if (lastRate < firstRate * 0.9f) {
            sb.append(String.format("  - Learning rate DECREASING: %.2f -> %.2f\n", firstRate, lastRate));
        } else {
            sb.append(String.format("  - Learning rate STABLE: %.2f\n", lastRate));
        }

        return sb.toString();
    }
}
