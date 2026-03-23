package com.lingfeng.sprite.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
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
import com.lingfeng.sprite.OwnerModel.CommunicationStyle;
import com.lingfeng.sprite.OwnerModel.Interaction;
import com.lingfeng.sprite.OwnerModel.InteractionType;
import com.lingfeng.sprite.OwnerModel.ProactiveFeedback;
import com.lingfeng.sprite.OwnerModel.Verbosity;

/**
 * 交互偏好学习服务 - S2-2: 交互偏好学习
 *
 * 功能：
 * 1. 分析主人对话风格（简短/详细/正式/随意）
 * 2. 记录主人最常用的交互时间
 * 3. 学习主人对不同类型消息的响应率
 * 4. 调整主动消息策略
 */
@Service
public class InteractionPreferenceLearningService {

    private static final Logger logger = LoggerFactory.getLogger(InteractionPreferenceLearningService.class);

    // 分析窗口：只分析最近30天的数据
    private static final int ANALYSIS_WINDOW_DAYS = 30;

    // 更新间隔：每小时更新一次偏好
    private static final long UPDATE_INTERVAL_HOURS = 1;

    // 最小样本量：至少需要这么多交互才能得出结论
    private static final int MIN_SAMPLES_FOR_INFERENCE = 5;

    private final UnifiedContextService unifiedContextService;
    private final FeedbackTrackerService feedbackTrackerService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final ZoneId TIMEZONE = ZoneId.of("Asia/Shanghai");

    // 缓存的偏好数据
    private volatile InteractionPreferences cachedPreferences = new InteractionPreferences();
    private volatile Instant lastUpdateTime = Instant.EPOCH;

    public InteractionPreferenceLearningService(
            @Autowired UnifiedContextService unifiedContextService,
            @Autowired FeedbackTrackerService feedbackTrackerService
    ) {
        this.unifiedContextService = unifiedContextService;
        this.feedbackTrackerService = feedbackTrackerService;

        // 启动定时更新
        scheduler.scheduleAtFixedRate(
                this::updatePreferences,
                UPDATE_INTERVAL_HOURS,
                UPDATE_INTERVAL_HOURS,
                TimeUnit.HOURS
        );

        // 初始更新
        updatePreferences();

        logger.info("InteractionPreferenceLearningService started - analysis window: {} days", ANALYSIS_WINDOW_DAYS);
    }

    /**
     * 主人交互偏好
     */
    public record InteractionPreferences(
            // 对话风格推断
            Verbosity inferredVerbosity,
            String inferredTone,

            // 最佳联系时间
            List<Integer> bestContactHours,       // 最佳联系小时（0-23）
            List<DayOfWeek> bestContactDays,      // 最佳联系星期

            // 触发类型响应率
            Map<String, Float> triggerResponseRates,  // triggerType -> response rate

            // 交互统计
            int totalInteractions,
            float avgResponseRate,
            int activeDays,

            // 置信度
            float verbosityConfidence,
            float toneConfidence,
            float bestTimeConfidence
    ) {
        public InteractionPreferences {
            if (inferredVerbosity == null) inferredVerbosity = Verbosity.MODERATE;
            if (inferredTone == null) inferredTone = "友好";
            if (bestContactHours == null) bestContactHours = List.of();
            if (bestContactDays == null) bestContactDays = List.of();
            if (triggerResponseRates == null) triggerResponseRates = Map.of();
        }
    }

    /**
     * 获取当前推断的交互偏好
     */
    public InteractionPreferences getPreferences() {
        return cachedPreferences;
    }

    /**
     * 获取主人偏好的消息风格描述
     */
    public String getPreferredMessageStyle() {
        InteractionPreferences prefs = cachedPreferences;
        StringBuilder style = new StringBuilder();

        // 语气
        style.append(prefs.inferredTone());

        // 详细程度
        switch (prefs.inferredVerbosity()) {
            case BRIEF -> style.append("、简短直接");
            case DETAILED -> style.append("、详细有条理");
            case MODERATE -> style.append("、适度");
        }

        return style.toString();
    }

    /**
     * 获取某小时的整体响应概率（基于历史数据）
     */
    public float getHourlyResponseProbability(int hour) {
        InteractionPreferences prefs = cachedPreferences;
        List<Integer> bestHours = prefs.bestContactHours();

        if (bestHours.contains(hour)) {
            // 在最佳时间内，响应概率较高
            return 0.7f + (prefs.bestTimeConfidence() * 0.2f);
        } else {
            // 不在最佳时间
            return 0.3f - (prefs.bestTimeConfidence() * 0.1f);
        }
    }

    /**
     * 获取某触发类型的响应概率
     */
    public float getTriggerResponseRate(String triggerType) {
        return cachedPreferences.triggerResponseRates()
                .getOrDefault(triggerType, cachedPreferences.avgResponseRate());
    }

    /**
     * 判断是否应该发送某种类型的主动消息
     */
    public boolean shouldSendProactiveMessage(String triggerType, int currentHour) {
        float triggerRate = getTriggerResponseRate(triggerType);
        float hourRate = getHourlyResponseProbability(currentHour);

        // 综合判断：两种概率都还可以才发送
        return triggerRate > 0.3f && hourRate > 0.35f;
    }

    /**
     * 获取调整后的消息长度建议
     */
    public int getPreferredMessageLength() {
        switch (cachedPreferences.inferredVerbosity()) {
            case BRIEF -> 20;      // 20字以内
            case DETAILED -> 100;  // 可以详细到100字
            default -> 50;         // 默认50字
        }
    }

    /**
     * 更新偏好数据
     */
    private synchronized void updatePreferences() {
        try {
            var worldOpt = unifiedContextService.getWorldModelOptional();
            if (worldOpt.isEmpty()) {
                return;
            }

            var world = worldOpt.get();
            if (world.owner() == null || world.owner().interactionHistory() == null) {
                return;
            }

            List<Interaction> interactions = world.owner().interactionHistory();
            List<ProactiveFeedback> feedbacks = feedbackTrackerService.getRecentFeedback(100);

            // 过滤最近30天的数据
            Instant windowStart = Instant.now().minusSeconds(ANALYSIS_WINDOW_DAYS * 24L * 60 * 60);

            List<Interaction> recentInteractions = interactions.stream()
                    .filter(i -> i.timestamp().isAfter(windowStart))
                    .toList();

            // 分析各种偏好
            Verbosity verbosity = inferVerbosity(recentInteractions);
            String tone = inferTone(recentInteractions);
            List<Integer> bestHours = inferBestContactHours(recentInteractions);
            List<DayOfWeek> bestDays = inferBestContactDays(recentInteractions);
            Map<String, Float> triggerRates = inferTriggerResponseRates(feedbacks);

            // 计算统计
            int totalInteractions = recentInteractions.size();
            float avgResponseRate = calculateAverageResponseRate(feedbacks);
            int activeDays = countActiveDays(recentInteractions);

            // 计算置信度
            float verbosityConf = calculateConfidence(totalInteractions, MIN_SAMPLES_FOR_INFERENCE);
            float toneConf = calculateConfidence(totalInteractions, MIN_SAMPLES_FOR_INFERENCE);
            float bestTimeConf = calculateConfidence(bestHours.size(), 3);

            cachedPreferences = new InteractionPreferences(
                    verbosity,
                    tone,
                    bestHours,
                    bestDays,
                    triggerRates,
                    totalInteractions,
                    avgResponseRate,
                    activeDays,
                    verbosityConf,
                    toneConf,
                    bestTimeConf
            );

            lastUpdateTime = Instant.now();
            logger.debug("Updated interaction preferences: verbosity={}, tone={}, bestHours={}, avgResponseRate={}",
                    verbosity, tone, bestHours, avgResponseRate);

        } catch (Exception e) {
            logger.warn("Failed to update interaction preferences: {}", e.getMessage());
        }
    }

    /**
     * 从对话历史推断主人的表达详细程度
     */
    private Verbosity inferVerbosity(List<Interaction> interactions) {
        if (interactions.size() < MIN_SAMPLES_FOR_INFERENCE) {
            return Verbosity.MODERATE;
        }

        // 分析消息长度
        List<Integer> messageLengths = new ArrayList<>();
        for (Interaction i : interactions) {
            if (i.content() != null) {
                messageLengths.add(i.content().length());
            }
        }

        if (messageLengths.isEmpty()) {
            return Verbosity.MODERATE;
        }

        double avgLength = messageLengths.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(50);

        if (avgLength < 20) {
            return Verbosity.BRIEF;
        } else if (avgLength > 80) {
            return Verbosity.DETAILED;
        } else {
            return Verbosity.MODERATE;
        }
    }

    /**
     * 从对话历史推断主人的语气偏好
     */
    private String inferTone(List<Interaction> interactions) {
        if (interactions.size() < MIN_SAMPLES_FOR_INFERENCE) {
            return "友好";
        }

        // 分析情感得分
        List<Float> sentiments = interactions.stream()
                .filter(i -> i.sentiment() > 0)
                .map(Interaction::sentiment)
                .toList();

        if (sentiments.isEmpty()) {
            return "友好";
        }

        double avgSentiment = sentiments.stream()
                .mapToDouble(Float::floatValue)
                .average()
                .orElse(0.5);

        if (avgSentiment >= 0.6) {
            return "热情";
        } else if (avgSentiment >= 0.4) {
            return "友好";
        } else if (avgSentiment >= 0.25) {
            return "中性";
        } else {
            return "冷淡";
        }
    }

    /**
     * 推断最佳联系时间（小时）
     */
    private List<Integer> inferBestContactHours(List<Interaction> interactions) {
        Map<Integer, Integer> hourCounts = new ConcurrentHashMap<>();

        // 统计每个小时的交互数量
        for (Interaction i : interactions) {
            LocalDateTime ldt = LocalDateTime.ofInstant(i.timestamp(), TIMEZONE);
            int hour = ldt.getHour();
            hourCounts.merge(hour, 1, Integer::sum);
        }

        if (hourCounts.isEmpty()) {
            // 默认最佳时间：上午9-11点，下午3-5点
            return List.of(9, 10, 11, 15, 16, 17);
        }

        // 找出交互最频繁的时间段
        return hourCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(6)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 推断最佳联系日期
     */
    private List<DayOfWeek> inferBestContactDays(List<Interaction> interactions) {
        Map<DayOfWeek, Integer> dayCounts = new ConcurrentHashMap<>();

        for (Interaction i : interactions) {
            DayOfWeek day = DayOfWeek.from(i.timestamp().atZone(TIMEZONE));
            dayCounts.merge(day, 1, Integer::sum);
        }

        if (dayCounts.isEmpty()) {
            // 默认工作日
            return List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        }

        // 找出最频繁的日期
        double avgCount = dayCounts.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(1);

        return dayCounts.entrySet().stream()
                .filter(e -> e.getValue() >= avgCount)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 推断不同触发类型的响应率
     */
    private Map<String, Float> inferTriggerResponseRates(List<ProactiveFeedback> feedbacks) {
        Map<String, List<ProactiveFeedback>> byTrigger = new HashMap<>();

        for (ProactiveFeedback fb : feedbacks) {
            byTrigger.computeIfAbsent(fb.triggerType(), k -> new ArrayList<>()).add(fb);
        }

        Map<String, Float> rates = new HashMap<>();
        for (Map.Entry<String, List<ProactiveFeedback>> entry : byTrigger.entrySet()) {
            List<ProactiveFeedback> fbs = entry.getValue();
            long positiveCount = fbs.stream()
                    .filter(fb -> fb.response() != ProactiveFeedback.ResponseType.IGNORE
                            && fb.response() != ProactiveFeedback.ResponseType.REJECT)
                    .count();
            rates.put(entry.getKey(), (float) positiveCount / fbs.size());
        }

        return rates;
    }

    /**
     * 计算平均响应率
     */
    private float calculateAverageResponseRate(List<ProactiveFeedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return 0.5f;
        }

        long positiveCount = feedbacks.stream()
                .filter(fb -> fb.response() != ProactiveFeedback.ResponseType.IGNORE
                        && fb.response() != ProactiveFeedback.ResponseType.REJECT)
                .count();

        return (float) positiveCount / feedbacks.size();
    }

    /**
     * 计算活跃天数
     */
    private int countActiveDays(List<Interaction> interactions) {
        return interactions.stream()
                .map(i -> i.timestamp().atZone(TIMEZONE).toLocalDate())
                .distinct()
                .toList()
                .size();
    }

    /**
     * 计算置信度
     */
    private float calculateConfidence(int sampleCount, int minRequired) {
        if (sampleCount <= 0) return 0f;
        return Math.min(1f, (float) sampleCount / (minRequired * 3));
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        logger.info("InteractionPreferenceLearningService shutdown complete");
    }
}
