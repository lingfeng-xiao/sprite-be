package com.lingfeng.sprite.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.OwnerModel.Mood;

/**
 * 情绪历史服务 - S3-1: 情绪历史追踪
 *
 * 功能：
 * 1. 记录主人情绪状态变化
 * 2. 按日期索引存储情绪历史
 * 3. 查询历史情绪模式
 * 4. 支持情绪趋势分析
 */
@Service
public class EmotionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(EmotionHistoryService.class);

    // 静态实例（用于从非Spring组件访问）
    private static volatile EmotionHistoryService instance = null;

    // 情绪历史记录（按日期索引）
    private final Map<LocalDate, List<EmotionRecord>> emotionHistoryByDate = new ConcurrentHashMap<>();

    // 情绪变化记录（用于追踪变化点）
    private final List<EmotionRecord> emotionChanges = new ArrayList<>();

    // 最新情绪状态
    private volatile EmotionRecord currentEmotion = null;

    // 保存阈值：只有情绪变化超过这个值才记录
    private static final float EMOTION_CHANGE_THRESHOLD = 0.2f;

    // 最大历史记录保留天数
    private static final int RETENTION_DAYS = 90;

    private final ZoneId TIMEZONE = ZoneId.of("Asia/Shanghai");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public EmotionHistoryService() {
        instance = this;

        // 每天清理过期记录
        scheduler.scheduleAtFixedRate(
                this::cleanupExpiredRecords,
                24,
                24,
                TimeUnit.HOURS
        );

        logger.info("EmotionHistoryService started - retention: {} days", RETENTION_DAYS);
    }

    /**
     * S3-1: 获取静态实例（供非Spring组件使用）
     */
    public static EmotionHistoryService getInstance() {
        return instance;
    }

    /**
     * 情绪记录
     */
    public record EmotionRecord(
            Instant timestamp,
            Mood mood,
            float intensity,
            String trigger,
            float sentimentScore
    ) {}

    /**
     * 情绪统计
     */
    public record EmotionStats(
            LocalDate date,
            Map<Mood, Long> moodCounts,
            float avgIntensity,
            long totalRecords,
            Mood mostCommonMood
    ) {}

    /**
     * 周模式统计
     */
    public record WeeklyPattern(
            Map<DayOfWeek, Mood> typicalMoods,
            Map<DayOfWeek, Float> avgIntensities,
            int[] moodDistribution
    ) {}

    /**
     * S3-2: 最优联系窗口
     */
    public record OptimalContactWindow(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            float score,
            Mood expectedMood,
            String reason
    ) {}

    /**
     * S3-2: 每周联系建议
     */
    public record WeeklyContactAdvice(
            List<OptimalContactWindow> bestWindows,
            List<DayOfWeek> avoidDays,
            String summary,
            int dataPointsAnalyzed
    ) {}

    /**
     * S3-3: 时间模式预测
     */
    public record TimePatternPrediction(
            LocalDate date,
            int hour,
            Mood predictedMood,
            float confidence,
            float contactScore,
            String basis
    ) {}

    /**
     * S3-3: 情绪趋势分析
     */
    public record EmotionTrend(
            String direction,      // "improving", "stable", "declining"
            float changeRate,     // 变化率
            int dataPoints,
            String summary
    ) {}

    /**
     * 记录当前情绪状态
     * 由 CognitionController 或感知系统调用
     */
    public void recordEmotion(Mood mood, float intensity, String trigger) {
        Instant now = Instant.now();
        LocalDate today = now.atZone(TIMEZONE).toLocalDate();

        // 计算情感分数
        float sentimentScore = calculateSentimentScore(mood, intensity);

        // 检查是否需要记录（情绪变化超过阈值）
        if (shouldRecord(mood, intensity)) {
            EmotionRecord record = new EmotionRecord(now, mood, intensity, trigger, sentimentScore);

            // 更新当前情绪
            currentEmotion = record;

            // 按日期存储
            emotionHistoryByDate.computeIfAbsent(today, k -> new ArrayList<>()).add(record);

            // 记录变化
            emotionChanges.add(record);

            // 只保留最近的变化记录
            if (emotionChanges.size() > 100) {
                emotionChanges.remove(0);
            }

            logger.debug("Recorded emotion: mood={}, intensity={}, trigger={}", mood, intensity, trigger);
        }
    }

    /**
     * 判断是否应该记录（情绪变化超过阈值）
     */
    private boolean shouldRecord(Mood mood, float intensity) {
        if (currentEmotion == null) {
            return true;
        }

        // 如果情绪类型变化
        if (currentEmotion.mood() != mood) {
            return true;
        }

        // 如果强度变化超过阈值
        if (Math.abs(currentEmotion.intensity() - intensity) > EMOTION_CHANGE_THRESHOLD) {
            return true;
        }

        return false;
    }

    /**
     * 计算情感分数
     */
    private float calculateSentimentScore(Mood mood, float intensity) {
        float baseScore = switch (mood) {
            case HAPPY, EXCITED, GRATEFUL, CONFIDENT -> 0.8f;
            case CALM, NEUTRAL -> 0.5f;
            case CONFUSED, TIRED -> 0.3f;
            case SAD, ANXIOUS, FRUSTRATED -> 0.1f;
        };
        return baseScore * intensity;
    }

    /**
     * 获取某日期的情绪统计
     */
    public EmotionStats getStatsForDate(LocalDate date) {
        List<EmotionRecord> records = emotionHistoryByDate.get(date);
        if (records == null || records.isEmpty()) {
            return null;
        }

        Map<Mood, Long> moodCounts = new HashMap<>();
        float totalIntensity = 0;

        for (EmotionRecord record : records) {
            moodCounts.merge(record.mood(), 1L, Long::sum);
            totalIntensity += record.intensity();
        }

        Mood mostCommon = moodCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Mood.NEUTRAL);

        return new EmotionStats(
                date,
                moodCounts,
                totalIntensity / records.size(),
                records.size(),
                mostCommon
        );
    }

    /**
     * 获取日期范围的情感趋势
     */
    public List<Float> getSentimentTrend(LocalDate startDate, LocalDate endDate) {
        List<Float> trend = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            EmotionStats stats = getStatsForDate(current);
            if (stats != null) {
                trend.add(stats.avgIntensity());
            } else {
                trend.add(0.5f); // 默认中性
            }
            current = current.plusDays(1);
        }

        return trend;
    }

    /**
     * 获取一周内的典型情绪模式
     */
    public WeeklyPattern getWeeklyPattern() {
        Map<DayOfWeek, List<Mood>> moodsByDay = new HashMap<>();
        Map<DayOfWeek, List<Float>> intensitiesByDay = new HashMap<>();

        for (Map.Entry<LocalDate, List<EmotionRecord>> entry : emotionHistoryByDate.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey().getDayOfWeek();
            List<EmotionRecord> records = entry.getValue();

            for (EmotionRecord record : records) {
                moodsByDay.computeIfAbsent(dayOfWeek, k -> new ArrayList<>()).add(record.mood());
                intensitiesByDay.computeIfAbsent(dayOfWeek, k -> new ArrayList<>()).add(record.intensity());
            }
        }

        Map<DayOfWeek, Mood> typicalMoods = new HashMap<>();
        Map<DayOfWeek, Float> avgIntensities = new HashMap<>();
        int[] moodDistribution = new int[Mood.values().length];

        for (DayOfWeek day : DayOfWeek.values()) {
            List<Mood> dayMoods = moodsByDay.get(day);
            if (dayMoods != null && !dayMoods.isEmpty()) {
                // 找出最常见的情绪
                Map<Mood, Long> countMap = new HashMap<>();
                for (Mood m : dayMoods) {
                    countMap.merge(m, 1L, Long::sum);
                }
                typicalMoods.put(day, countMap.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(Mood.NEUTRAL));

                // 计算平均强度
                float avgIntensity = intensitiesByDay.get(day).stream()
                        .mapToFloat(Float::floatValue)
                        .average()
                        .orElse(0.5f);
                avgIntensities.put(day, avgIntensity);
            }
        }

        // 统计总体情绪分布
        for (List<EmotionRecord> records : emotionHistoryByDate.values()) {
            for (EmotionRecord record : records) {
                moodDistribution[record.mood().ordinal()]++;
            }
        }

        return new WeeklyPattern(typicalMoods, avgIntensities, moodDistribution);
    }

    /**
     * S3-2: 获取最优联系时间窗口
     * 分析历史数据，找出主人情绪最积极、最适合主动联系的时间段
     */
    public List<OptimalContactWindow> getOptimalContactWindows() {
        List<OptimalContactWindow> windows = new ArrayList<>();

        // 分析最近30天的数据
        List<EmotionRecord> recentRecords = getRecentRecords(30);
        if (recentRecords.isEmpty()) {
            return windows;
        }

        // 按星期和小时分组分析
        Map<DayOfWeek, Map<Integer, List<EmotionRecord>>> recordsByDayAndHour = new HashMap<>();

        for (EmotionRecord record : recentRecords) {
            LocalDate date = record.timestamp().atZone(TIMEZONE).toLocalDate();
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            int hour = record.timestamp().atZone(TIMEZONE).getHour();

            recordsByDayAndHour
                    .computeIfAbsent(dayOfWeek, k -> new HashMap<>())
                    .computeIfAbsent(hour, k -> new ArrayList<>())
                    .add(record);
        }

        // 对每个星期和小时组合计算分数
        for (Map.Entry<DayOfWeek, Map<Integer, List<EmotionRecord>>> dayEntry : recordsByDayAndHour.entrySet()) {
            DayOfWeek day = dayEntry.getKey();
            Map<Integer, List<EmotionRecord>> hourRecords = dayEntry.getValue();

            for (Map.Entry<Integer, List<EmotionRecord>> hourEntry : hourRecords.entrySet()) {
                int hour = hourEntry.getKey();
                List<EmotionRecord> records = hourEntry.getValue();

                // 计算该时间窗口的情绪分数
                float avgSentiment = records.stream()
                        .mapToFloat(EmotionRecord::sentimentScore)
                        .average()
                        .orElse(0.5f);

                // 计算积极情绪占比
                long positiveCount = records.stream()
                        .filter(r -> isPositiveMood(r.mood()))
                        .count();
                float positiveRatio = (float) positiveCount / records.size();

                // 综合分数：情绪分数 * 0.6 + 积极比例 * 0.4
                float score = avgSentiment * 0.6f + positiveRatio * 0.4f;

                // 找出最常见的情绪
                Mood typicalMood = records.stream()
                        .map(EmotionRecord::mood)
                        .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
                        .entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(Mood.NEUTRAL);

                // 只返回分数 > 0.5 的时间窗口
                if (score > 0.5f) {
                    String reason = String.format("历史数据：%d条记录，平均情绪分数%.2f，积极情绪占比%.0f%%",
                            records.size(), avgSentiment, positiveRatio * 100);

                    windows.add(new OptimalContactWindow(
                            day,
                            LocalTime.of(hour, 0),
                            LocalTime.of(hour, 59),
                            score,
                            typicalMood,
                            reason
                    ));
                }
            }
        }

        // 按分数降序排序
        windows.sort((a, b) -> Float.compare(b.score(), a.score()));

        return windows;
    }

    /**
     * S3-2: 判断是否为积极情绪
     */
    private boolean isPositiveMood(Mood mood) {
        return switch (mood) {
            case HAPPY, EXCITED, GRATEFUL, CONFIDENT -> true;
            default -> false;
        };
    }

    /**
     * S3-2: 判断是否为消极情绪
     */
    private boolean isNegativeMood(Mood mood) {
        return switch (mood) {
            case SAD, ANXIOUS, FRUSTRATED -> true;
            default -> false;
        };
    }

    /**
     * S3-2: 获取每周联系建议
     * 综合分析给出最佳联系时间建议
     */
    public WeeklyContactAdvice getWeeklyContactAdvice() {
        List<OptimalContactWindow> optimalWindows = getOptimalContactWindows();

        // 统计应避免联系的日期
        List<DayOfWeek> avoidDays = new ArrayList<>();
        Map<DayOfWeek, Float> dayScores = new HashMap<>();

        for (DayOfWeek day : DayOfWeek.values()) {
            List<OptimalContactWindow> dayWindows = optimalWindows.stream()
                    .filter(w -> w.dayOfWeek() == day)
                    .toList();

            if (dayWindows.isEmpty()) {
                // 没有积极数据，可能是联系效果不好的日期
                avoidDays.add(day);
            } else {
                float avgScore = dayWindows.stream()
                        .mapToFloat(OptimalContactWindow::score)
                        .average()
                        .orElse(0f);
                dayScores.put(day, avgScore);
            }
        }

        // 生成总结
        String summary;
        if (optimalWindows.isEmpty()) {
            summary = "数据不足，需要更多历史数据来分析最佳联系时间";
        } else {
            // 找出最佳窗口
            OptimalContactWindow best = optimalWindows.get(0);
            summary = String.format("本周最佳联系时间：%s %s-%s，预估情绪%s，成功率较高",
                    getDayName(best.dayOfWeek()),
                    best.startTime().toString(),
                    best.endTime().toString(),
                    getMoodName(best.expectedMood()));
        }

        int dataPoints = getRecentRecords(30).size();

        return new WeeklyContactAdvice(
                optimalWindows.stream().limit(5).toList(), // 最多返回5个最佳窗口
                avoidDays,
                summary,
                dataPoints
        );
    }

    /**
     * S3-2: 获取某日期的情绪预测
     * 基于历史数据预测某天的情绪
     */
    public Mood getPredictedMoodForDay(DayOfWeek dayOfWeek) {
        List<EmotionRecord> recentRecords = getRecentRecords(30);

        // 按星期筛选
        List<Mood> dayMoods = recentRecords.stream()
                .filter(r -> r.timestamp().atZone(TIMEZONE).getDayOfWeek() == dayOfWeek)
                .map(EmotionRecord::mood)
                .toList();

        if (dayMoods.isEmpty()) {
            return Mood.NEUTRAL;
        }

        // 返回最常见的情绪
        return dayMoods.stream()
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Mood.NEUTRAL);
    }

    /**
     * S3-2: 获取某时间的情绪预测分数
     * 返回 0.0-1.0，分数越高越适合联系
     */
    public float getPredictedContactScore(DayOfWeek dayOfWeek, int hour) {
        List<OptimalContactWindow> windows = getOptimalContactWindows();

        return windows.stream()
                .filter(w -> w.dayOfWeek() == dayOfWeek && w.startTime().getHour() == hour)
                .findFirst()
                .map(OptimalContactWindow::score)
                .orElse(0.5f); // 默认中性
    }

    /**
     * S3-3: 预测指定日期和时间的情绪状态
     */
    public TimePatternPrediction predictEmotion(LocalDate date, int hour) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<EmotionRecord> recentRecords = getRecentRecords(30);

        // 按星期和小时筛选
        List<EmotionRecord> matchingRecords = recentRecords.stream()
                .filter(r -> r.timestamp().atZone(TIMEZONE).getDayOfWeek() == dayOfWeek)
                .filter(r -> r.timestamp().atZone(TIMEZONE).getHour() == hour)
                .toList();

        Mood predictedMood;
        float confidence;
        String basis;

        if (matchingRecords.isEmpty()) {
            // 没有精确匹配，使用当天的普遍情绪
            predictedMood = getPredictedMoodForDay(dayOfWeek);
            confidence = 0.3f;
            basis = "基于" + dayOfWeek + "普遍情绪推测";
        } else {
            // 计算最常见的情绪
            predictedMood = matchingRecords.stream()
                    .map(EmotionRecord::mood)
                    .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(Mood.NEUTRAL);

            // 置信度基于样本数量
            confidence = Math.min(0.9f, matchingRecords.size() * 0.1f + 0.3f);
            basis = "基于" + matchingRecords.size() + "条历史记录";
        }

        float contactScore = getPredictedContactScore(dayOfWeek, hour);

        return new TimePatternPrediction(date, hour, predictedMood, confidence, contactScore, basis);
    }

    /**
     * S3-3: 获取情绪趋势分析
     * 分析最近N天的情绪变化趋势
     */
    public EmotionTrend getEmotionTrend(int days) {
        List<EmotionRecord> recentRecords = getRecentRecords(days);

        if (recentRecords.size() < 3) {
            return new EmotionTrend("stable", 0f, recentRecords.size(), "数据不足，无法判断趋势");
        }

        // 按日期分组计算平均情绪分数
        LocalDate today = LocalDate.now(TIMEZONE);
        Map<LocalDate, Float> dailyScores = new HashMap<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = today.minusDays(i);
            List<EmotionRecord> dayRecords = emotionHistoryByDate.get(date);
            if (dayRecords != null && !dayRecords.isEmpty()) {
                float avgScore = dayRecords.stream()
                        .mapToFloat(EmotionRecord::sentimentScore)
                        .average()
                        .orElse(0.5f);
                dailyScores.put(date, avgScore);
            }
        }

        if (dailyScores.size() < 2) {
            return new EmotionTrend("stable", 0f, dailyScores.size(), "数据不足，无法判断趋势");
        }

        // 计算趋势
        List<Map.Entry<LocalDate, Float>> sorted = new ArrayList<>(dailyScores.entrySet());
        sorted.sort(Map.Entry.comparingByKey());

        // 比较前半部分和后半部分的平均分
        int mid = sorted.size() / 2;
        float firstHalfAvg = sorted.subList(0, mid).stream()
                .mapToDouble(Map.Entry::getValue)
                .average()
                .orElse(0.5f);
        float secondHalfAvg = sorted.subList(mid, sorted.size()).stream()
                .mapToDouble(Map.Entry::getValue)
                .average()
                .orElse(0.5f);

        float changeRate = secondHalfAvg - firstHalfAvg;

        String direction;
        if (changeRate > 0.1f) {
            direction = "improving";
        } else if (changeRate < -0.1f) {
            direction = "declining";
        } else {
            direction = "stable";
        }

        String summary;
        if (changeRate > 0.1f) {
            summary = String.format("情绪状态呈改善趋势，提升%.0f%%", changeRate * 100);
        } else if (changeRate < -0.1f) {
            summary = String.format("情绪状态需关注，下降%.0f%%", Math.abs(changeRate) * 100);
        } else {
            summary = "情绪状态基本稳定";
        }

        return new EmotionTrend(direction, changeRate, dailyScores.size(), summary);
    }

    /**
     * S3-3: 预测明天的情绪状态
     */
    public TimePatternPrediction predictTomorrowEmotion(int hour) {
        LocalDate tomorrow = LocalDate.now(TIMEZONE).plusDays(1);
        return predictEmotion(tomorrow, hour);
    }

    private String getDayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private String getMoodName(Mood mood) {
        return switch (mood) {
            case HAPPY -> "开心";
            case EXCITED -> "兴奋";
            case GRATEFUL -> "感激";
            case CONFIDENT -> "自信";
            case CALM -> "平静";
            case NEUTRAL -> "中性";
            case CONFUSED -> "困惑";
            case TIRED -> "疲惫";
            case SAD -> "难过";
            case ANXIOUS -> "焦虑";
            case FRUSTRATED -> "烦躁";
        };
    }

    /**
     * 获取最近N天的情绪记录
     */
    public List<EmotionRecord> getRecentRecords(int days) {
        List<EmotionRecord> recent = new ArrayList<>();
        LocalDate today = LocalDate.now(TIMEZONE);

        for (int i = 0; i < days; i++) {
            LocalDate date = today.minusDays(i);
            List<EmotionRecord> dayRecords = emotionHistoryByDate.get(date);
            if (dayRecords != null) {
                recent.addAll(dayRecords);
            }
        }

        return recent;
    }

    /**
     * 获取某情绪类型的历史记录
     */
    public List<EmotionRecord> getRecordsByMood(Mood mood, int limit) {
        return emotionChanges.stream()
                .filter(r -> r.mood() == mood)
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .limit(limit)
                .toList();
    }

    /**
     * 获取当前情绪
     */
    public EmotionRecord getCurrentEmotion() {
        return currentEmotion;
    }

    /**
     * 清理过期记录
     */
    private void cleanupExpiredRecords() {
        LocalDate cutoff = LocalDate.now(TIMEZONE).minusDays(RETENTION_DAYS);
        emotionHistoryByDate.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoff));

        // 清理旧的变更记录
        Instant cutoffInstant = cutoff.atStartOfDay(TIMEZONE).toInstant();
        emotionChanges.removeIf(r -> r.timestamp().isBefore(cutoffInstant));

        logger.debug("Cleaned up emotion records older than {}", cutoff);
    }

    /**
     * 获取情绪历史记录数
     */
    public int getTotalRecordCount() {
        return emotionHistoryByDate.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        logger.info("EmotionHistoryService shutdown complete");
    }
}
