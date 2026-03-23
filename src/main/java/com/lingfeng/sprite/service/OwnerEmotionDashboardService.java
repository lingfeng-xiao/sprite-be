package com.lingfeng.sprite.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S10-4: 主人情绪历史Dashboard服务
 *
 * 提供主人情绪历史的可视化数据：
 * - 情绪趋势图
 * - 情绪分布统计
 * - 周内模式分析
 * - 最优联系时间建议
 */
public class OwnerEmotionDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(OwnerEmotionDashboardService.class);

    private final List<EmotionSnapshot> emotionHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 500;

    /**
     * 情绪快照
     */
    public record EmotionSnapshot(
        Instant timestamp,
        float sentiment,
        String emotion,
        String trigger,
        String context
    ) {}

    /**
     * 情绪分布
     */
    public record EmotionDistribution(
        int positiveCount,
        int neutralCount,
        int negativeCount,
        float positivePercent,
        float neutralPercent,
        float negativePercent
    ) {}

    /**
     * 情绪趋势数据点
     */
    public record EmotionTrendPoint(
        Instant timestamp,
        float sentiment,
        String emotion
    ) {}

    /**
     * 周内模式
     */
    public record WeeklyPattern(
        int dayOfWeek,
        String dayName,
        float averageSentiment,
        int interactionCount,
        float emotionalStability
    ) {}

    /**
     * 主人情绪Dashboard数据
     */
    public record OwnerEmotionDashboardData(
        Instant timestamp,
        EmotionDistribution distribution,
        List<EmotionTrendPoint> recentTrend,
        WeeklyPattern[] weeklyPatterns,
        float averageSentiment,
        float sentimentVolatility,
        String currentEmotion,
        OptimalContactTime[] optimalTimes
    ) {}

    /**
     * 最优联系时间
     */
    public record OptimalContactTime(
        int hourOfDay,
        String timeSlot,
        float score,
        String reason
    ) {}

    public OwnerEmotionDashboardService() {}

    // ==================== 情绪记录 ====================

    /**
     * 记录情绪
     */
    public void recordEmotion(float sentiment, String emotion, String trigger, String context) {
        EmotionSnapshot snapshot = new EmotionSnapshot(
            Instant.now(),
            sentiment,
            emotion,
            trigger,
            context
        );
        emotionHistory.add(snapshot);

        while (emotionHistory.size() > MAX_HISTORY) {
            emotionHistory.remove(0);
        }
    }

    /**
     * 记录情绪（简化版）
     */
    public void recordEmotion(float sentiment, String emotion) {
        recordEmotion(sentiment, emotion, null, null);
    }

    // ==================== 数据生成 ====================

    /**
     * 生成Dashboard数据（示例实现）
     * 实际数据需要从EmotionHistoryService获取
     */
    public OwnerEmotionDashboardData generateDashboardData(
        List<?> emotionHistoryData,
        List<?> weeklyAdviceData
    ) {
        Instant now = Instant.now();

        // 计算情绪分布
        EmotionDistribution distribution = calculateDistribution();

        // 生成趋势数据（最近24小时）
        List<EmotionTrendPoint> recentTrend = generateRecentTrend();

        // 生成周内模式
        WeeklyPattern[] weeklyPatterns = generateWeeklyPatterns();

        // 计算平均情绪
        float avgSentiment = emotionHistory.isEmpty() ? 0 :
            (float) emotionHistory.stream()
                .mapToDouble(EmotionSnapshot::sentiment)
                .average().orElse(0);

        // 计算情绪波动性
        float volatility = calculateVolatility();

        // 当前情绪
        String currentEmotion = emotionHistory.isEmpty() ? "unknown" :
            emotionHistory.get(emotionHistory.size() - 1).emotion();

        // 最优联系时间
        OptimalContactTime[] optimalTimes = generateOptimalTimes();

        return new OwnerEmotionDashboardData(
            now,
            distribution,
            recentTrend,
            weeklyPatterns,
            avgSentiment,
            volatility,
            currentEmotion,
            optimalTimes
        );
    }

    /**
     * 计算情绪分布
     */
    private EmotionDistribution calculateDistribution() {
        if (emotionHistory.isEmpty()) {
            return new EmotionDistribution(0, 0, 0, 0, 0, 0);
        }

        int positive = 0, neutral = 0, negative = 0;
        for (EmotionSnapshot e : emotionHistory) {
            if (e.sentiment() > 0.2f) positive++;
            else if (e.sentiment() < -0.2f) negative++;
            else neutral++;
        }

        int total = emotionHistory.size();
        return new EmotionDistribution(
            positive, neutral, negative,
            positive * 100f / total,
            neutral * 100f / total,
            negative * 100f / total
        );
    }

    /**
     * 生成最近趋势
     */
    private List<EmotionTrendPoint> generateRecentTrend() {
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        return emotionHistory.stream()
            .filter(e -> e.timestamp().isAfter(oneDayAgo))
            .map(e -> new EmotionTrendPoint(e.timestamp(), e.sentiment(), e.emotion()))
            .toList();
    }

    /**
     * 生成周内模式
     */
    private WeeklyPattern[] generateWeeklyPatterns() {
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        WeeklyPattern[] patterns = new WeeklyPattern[7];

        ZoneId timezone = ZoneId.of("Asia/Shanghai");

        for (int i = 0; i < 7; i++) {
            int dayOfWeek = i + 1;
            final int targetDay = dayOfWeek;

            List<EmotionSnapshot> dayEmotions = emotionHistory.stream()
                .filter(e -> e.timestamp().atZone(timezone).getDayOfWeek().getValue() == targetDay)
                .toList();

            float avgSentiment = dayEmotions.isEmpty() ? 0 :
                (float) dayEmotions.stream().mapToDouble(EmotionSnapshot::sentiment).average().orElse(0);

            float stability = calculateStability(dayEmotions);

            patterns[i] = new WeeklyPattern(
                dayOfWeek,
                dayNames[i],
                avgSentiment,
                dayEmotions.size(),
                stability
            );
        }

        return patterns;
    }

    /**
     * 计算情绪波动性
     */
    private float calculateVolatility() {
        if (emotionHistory.size() < 2) return 0f;

        double mean = emotionHistory.stream()
            .mapToDouble(EmotionSnapshot::sentiment)
            .average().orElse(0);

        double variance = emotionHistory.stream()
            .mapToDouble(e -> Math.pow(e.sentiment() - mean, 2))
            .average().orElse(0);

        return (float) Math.sqrt(variance);
    }

    /**
     * 计算稳定性
     */
    private float calculateStability(List<EmotionSnapshot> emotions) {
        if (emotions.size() < 2) return 1f;

        double mean = emotions.stream()
            .mapToDouble(EmotionSnapshot::sentiment)
            .average().orElse(0);

        double variance = emotions.stream()
            .mapToDouble(e -> Math.pow(e.sentiment() - mean, 2))
            .average().orElse(0);

        // 转换为稳定性分数（低方差 = 高稳定性）
        return (float) Math.max(0, 1 - Math.sqrt(variance));
    }

    /**
     * 生成最优联系时间
     */
    private OptimalContactTime[] generateOptimalTimes() {
        ZoneId timezone = ZoneId.of("Asia/Shanghai");

        // 按小时统计
        double[] hourSentiments = new double[24];
        int[] hourCounts = new int[24];

        for (EmotionSnapshot e : emotionHistory) {
            int hour = e.timestamp().atZone(timezone).getHour();
            hourSentiments[hour] += e.sentiment();
            hourCounts[hour]++;
        }

        // 计算平均情绪并排序
        List<OptimalContactTime> times = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            if (hourCounts[hour] > 0) {
                float avgSentiment = (float) (hourSentiments[hour] / hourCounts[hour]);
                // 分数 = (情绪值 + 1) / 2 * 100，限制在0-100
                float score = Math.max(0, Math.min(100, (avgSentiment + 1) * 50));

                String timeSlot = String.format("%02d:00-%02d:00", hour, (hour + 1) % 24);
                String reason = avgSentiment > 0.2 ? "主人情绪较好" :
                               avgSentiment < -0.2 ? "主人情绪较低" : "主人情绪平稳";

                times.add(new OptimalContactTime(hour, timeSlot, score, reason));
            }
        }

        // 按分数排序，取前5
        return times.stream()
            .sorted((a, b) -> Float.compare(b.score(), a.score()))
            .limit(5)
            .toArray(OptimalContactTime[]::new);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取情绪历史
     */
    public List<EmotionSnapshot> getHistory() {
        return new ArrayList<>(emotionHistory);
    }

    /**
     * 获取指定天数的历史
     */
    public List<EmotionSnapshot> getHistory(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return emotionHistory.stream()
            .filter(e -> e.timestamp().isAfter(cutoff))
            .toList();
    }

    /**
     * 获取情绪摘要
     */
    public String getEmotionSummary() {
        if (emotionHistory.isEmpty()) {
            return "No emotion data recorded";
        }

        EmotionDistribution dist = calculateDistribution();

        StringBuilder sb = new StringBuilder();
        sb.append("Owner Emotion Summary (Last 7 days):\n");
        sb.append(String.format("  Distribution: Positive=%.1f%%, Neutral=%.1f%%, Negative=%.1f%%\n",
            dist.positivePercent(), dist.neutralPercent(), dist.negativePercent()));

        EmotionSnapshot last = emotionHistory.get(emotionHistory.size() - 1);
        sb.append(String.format("  Current: %s (%.2f)\n", last.emotion(), last.sentiment()));

        float avg = (float) emotionHistory.stream()
            .mapToDouble(EmotionSnapshot::sentiment)
            .average().orElse(0);
        sb.append(String.format("  Average: %.2f\n", avg));

        return sb.toString();
    }
}
