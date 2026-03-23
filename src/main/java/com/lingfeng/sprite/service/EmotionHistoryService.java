package com.lingfeng.sprite.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
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
