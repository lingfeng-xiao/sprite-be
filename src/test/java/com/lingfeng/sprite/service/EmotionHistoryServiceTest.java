package com.lingfeng.sprite.service;

import com.lingfeng.sprite.OwnerModel.Mood;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmotionHistoryService 单元测试 - S3 情绪历史追踪
 */
class EmotionHistoryServiceTest {

    private EmotionHistoryService service;

    @BeforeEach
    void setUp() {
        service = new EmotionHistoryService();
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    // ==================== 基础记录测试 ====================

    @Test
    void testRecordEmotion_firstRecord_shouldBeRecorded() {
        service.recordEmotion(Mood.HAPPY, 0.8f, "收到好消息");
        EmotionHistoryService.EmotionRecord current = service.getCurrentEmotion();

        assertNotNull(current);
        assertEquals(Mood.HAPPY, current.mood());
        assertEquals(0.8f, current.intensity());
        assertEquals("收到好消息", current.trigger());
    }

    @Test
    void testRecordEmotion_sameMoodAndIntensity_shouldNotRecord() {
        service.recordEmotion(Mood.HAPPY, 0.8f, "test1");
        int count1 = service.getTotalRecordCount();

        service.recordEmotion(Mood.HAPPY, 0.8f, "test2");
        int count2 = service.getTotalRecordCount();

        // 同样的情绪和强度不应该重复记录
        assertEquals(count1, count2);
    }

    @Test
    void testRecordEmotion_differentMood_shouldRecord() {
        service.recordEmotion(Mood.HAPPY, 0.8f, "test1");
        int count1 = service.getTotalRecordCount();

        service.recordEmotion(Mood.SAD, 0.6f, "test2");
        int count2 = service.getTotalRecordCount();

        assertEquals(count1 + 1, count2);
    }

    @Test
    void testRecordEmotion_intensityChangeExceedsThreshold_shouldRecord() {
        service.recordEmotion(Mood.HAPPY, 0.5f, "test1");
        int count1 = service.getTotalRecordCount();

        // 强度变化超过0.2阈值
        service.recordEmotion(Mood.HAPPY, 0.8f, "test2");
        int count2 = service.getTotalRecordCount();

        assertEquals(count1 + 1, count2);
    }

    @Test
    void testGetCurrentEmotion_noRecord_returnsNull() {
        // 静态实例可能为null因为构造函数后才设置
        EmotionHistoryService.EmotionRecord current = service.getCurrentEmotion();
        assertNull(current);
    }

    @Test
    void testGetTotalRecordCount_empty_returnsZero() {
        assertEquals(0, service.getTotalRecordCount());
    }

    // ==================== 情绪统计测试 ====================

    @Test
    void testGetStatsForDate_noData_returnsNull() {
        LocalDate today = LocalDate.now();
        EmotionHistoryService.EmotionStats stats = service.getStatsForDate(today);
        assertNull(stats);
    }

    @Test
    void testGetStatsForDate_withData_returnsStats() {
        LocalDate today = LocalDate.now();

        service.recordEmotion(Mood.HAPPY, 0.8f, "test1");
        service.recordEmotion(Mood.HAPPY, 0.7f, "test2");
        service.recordEmotion(Mood.SAD, 0.5f, "test3");

        EmotionHistoryService.EmotionStats stats = service.getStatsForDate(today);

        assertNotNull(stats);
        assertEquals(today, stats.date());
        assertEquals(3, stats.totalRecords());
        assertEquals(Mood.HAPPY, stats.mostCommonMood());
    }

    @Test
    void testGetSentimentTrend_noData_returnsNeutral() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        List<Float> trend = service.getSentimentTrend(start, end);

        assertNotNull(trend);
        assertEquals(8, trend.size()); // 7 days + today
        // 所有日期都应该返回0.5f (中性)
        assertTrue(trend.stream().allMatch(f -> f == 0.5f));
    }

    // ==================== 周模式测试 ====================

    @Test
    void testGetWeeklyPattern_noData_returnsEmptyPattern() {
        EmotionHistoryService.WeeklyPattern pattern = service.getWeeklyPattern();

        assertNotNull(pattern);
        assertNotNull(pattern.typicalMoods());
        assertNotNull(pattern.avgIntensities());
        assertNotNull(pattern.moodDistribution());
        assertEquals(0, pattern.moodDistribution()[0]); // 空数据
    }

    @Test
    void testGetWeeklyPattern_withData_returnsPattern() {
        // 记录一些数据
        service.recordEmotion(Mood.HAPPY, 0.8f, "test1");
        service.recordEmotion(Mood.CALM, 0.6f, "test2");

        EmotionHistoryService.WeeklyPattern pattern = service.getWeeklyPattern();

        assertNotNull(pattern);
        assertTrue(pattern.moodDistribution().length > 0);
    }

    // ==================== 最优联系时间测试 ====================

    @Test
    void testGetOptimalContactWindows_noData_returnsEmptyList() {
        List<EmotionHistoryService.OptimalContactWindow> windows = service.getOptimalContactWindows();
        assertNotNull(windows);
        assertTrue(windows.isEmpty());
    }

    @Test
    void testGetOptimalContactWindows_withPositiveData_returnsWindows() {
        // 记录积极情绪数据
        service.recordEmotion(Mood.HAPPY, 0.9f, "positive1");
        service.recordEmotion(Mood.EXCITED, 0.8f, "positive2");

        List<EmotionHistoryService.OptimalContactWindow> windows = service.getOptimalContactWindows();
        assertNotNull(windows);
    }

    // ==================== 每周联系建议测试 ====================

    @Test
    void testGetWeeklyContactAdvice_noData_returnsAdvice() {
        EmotionHistoryService.WeeklyContactAdvice advice = service.getWeeklyContactAdvice();

        assertNotNull(advice);
        assertNotNull(advice.bestWindows());
        assertNotNull(advice.avoidDays());
        assertNotNull(advice.summary());
        assertEquals(0, advice.dataPointsAnalyzed());
        assertTrue(advice.summary().contains("数据不足"));
    }

    // ==================== 情绪预测测试 ====================

    @Test
    void testGetPredictedMoodForDay_noData_returnsNeutral() {
        Mood predicted = service.getPredictedMoodForDay(DayOfWeek.MONDAY);
        assertEquals(Mood.NEUTRAL, predicted);
    }

    @Test
    void testGetPredictedContactScore_default_returnsNeutral() {
        float score = service.getPredictedContactScore(DayOfWeek.MONDAY, 10);
        assertEquals(0.5f, score); // 默认中性
    }

    @Test
    void testPredictEmotion_noData_returnsNeutralWithLowConfidence() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        EmotionHistoryService.TimePatternPrediction prediction = service.predictEmotion(tomorrow, 10);

        assertNotNull(prediction);
        assertEquals(tomorrow, prediction.date());
        assertEquals(10, prediction.hour());
        assertEquals(Mood.NEUTRAL, prediction.predictedMood());
        assertEquals(0.3f, prediction.confidence()); // 低置信度
        assertTrue(prediction.basis().contains("推测"));
    }

    @Test
    void testPredictTomorrowEmotion_callsPredictEmotion() {
        // 不抛异常即通过
        assertDoesNotThrow(() -> {
            EmotionHistoryService.TimePatternPrediction prediction = service.predictTomorrowEmotion(10);
            assertNotNull(prediction);
        });
    }

    // ==================== 情绪趋势测试 ====================

    @Test
    void testGetEmotionTrend_noData_returnsStable() {
        EmotionHistoryService.EmotionTrend trend = service.getEmotionTrend(7);

        assertNotNull(trend);
        assertEquals("stable", trend.direction());
        assertEquals(0f, trend.changeRate());
        assertTrue(trend.summary().contains("数据不足"));
    }

    @Test
    void testGetEmotionTrend_insufficientData_returnsStable() {
        service.recordEmotion(Mood.HAPPY, 0.8f, "test1");

        EmotionHistoryService.EmotionTrend trend = service.getEmotionTrend(7);

        assertNotNull(trend);
        assertEquals("stable", trend.direction());
    }

    // ==================== 记录获取测试 ====================

    @Test
    void testGetRecentRecords_noData_returnsEmptyList() {
        List<EmotionHistoryService.EmotionRecord> records = service.getRecentRecords(30);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    void testGetRecordsByMood_noData_returnsEmptyList() {
        List<EmotionHistoryService.EmotionRecord> records = service.getRecordsByMood(Mood.HAPPY, 10);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    void testGetRecordsByMood_withData_returnsFilteredRecords() {
        service.recordEmotion(Mood.HAPPY, 0.8f, "test1");
        service.recordEmotion(Mood.SAD, 0.5f, "test2");

        List<EmotionHistoryService.EmotionRecord> happyRecords = service.getRecordsByMood(Mood.HAPPY, 10);

        assertNotNull(happyRecords);
        assertEquals(1, happyRecords.size());
        assertEquals(Mood.HAPPY, happyRecords.get(0).mood());
    }

    @Test
    void testGetRecordsByMood_withLimit_returnsLimitedRecords() {
        // 记录多条同一情绪的记录
        for (int i = 0; i < 5; i++) {
            service.recordEmotion(Mood.HAPPY, 0.8f, "test" + i);
        }

        List<EmotionHistoryService.EmotionRecord> records = service.getRecordsByMood(Mood.HAPPY, 3);

        assertNotNull(records);
        assertEquals(3, records.size());
    }

    // ==================== 辅助方法测试 ====================

    @Test
    void testSentimentScore_calculation() {
        service.recordEmotion(Mood.HAPPY, 1.0f, "test");
        EmotionHistoryService.EmotionRecord record = service.getCurrentEmotion();

        assertNotNull(record);
        // HAPPY base = 0.8, intensity = 1.0, score = 0.8
        assertEquals(0.8f, record.sentimentScore(), 0.01f);
    }

    @Test
    void testSentimentScore_negativeMood() {
        service.recordEmotion(Mood.SAD, 1.0f, "test");
        EmotionHistoryService.EmotionRecord record = service.getCurrentEmotion();

        assertNotNull(record);
        // SAD base = 0.1, intensity = 1.0, score = 0.1
        assertEquals(0.1f, record.sentimentScore(), 0.01f);
    }

    @Test
    void testSentimentScore_neutralMood() {
        service.recordEmotion(Mood.NEUTRAL, 0.5f, "test");
        EmotionHistoryService.EmotionRecord record = service.getCurrentEmotion();

        assertNotNull(record);
        // NEUTRAL base = 0.5, intensity = 0.5, score = 0.25
        assertEquals(0.25f, record.sentimentScore(), 0.01f);
    }

    // ==================== 记录类型测试 ====================

    @Test
    void testEmotionRecord_immutable() {
        service.recordEmotion(Mood.HAPPY, 0.8f, "test");
        EmotionHistoryService.EmotionRecord record = service.getCurrentEmotion();

        assertNotNull(record.timestamp());
        assertEquals(Mood.HAPPY, record.mood());
        assertEquals(0.8f, record.intensity());
        assertEquals("test", record.trigger());
    }

    @Test
    void testEmotionStats_immutable() {
        LocalDate today = LocalDate.now();
        EmotionHistoryService.EmotionStats stats = new EmotionHistoryService.EmotionStats(
                today,
                java.util.Map.of(Mood.HAPPY, 5L),
                0.7f,
                10L,
                Mood.HAPPY
        );

        assertEquals(today, stats.date());
        assertEquals(5L, stats.moodCounts().get(Mood.HAPPY));
        assertEquals(0.7f, stats.avgIntensity());
        assertEquals(10L, stats.totalRecords());
        assertEquals(Mood.HAPPY, stats.mostCommonMood());
    }

    @Test
    void testWeeklyPattern_immutable() {
        EmotionHistoryService.WeeklyPattern pattern = new EmotionHistoryService.WeeklyPattern(
                java.util.Map.of(DayOfWeek.MONDAY, Mood.HAPPY),
                java.util.Map.of(DayOfWeek.MONDAY, 0.7f),
                new int[]{1, 2, 3}
        );

        assertEquals(Mood.HAPPY, pattern.typicalMoods().get(DayOfWeek.MONDAY));
        assertEquals(0.7f, pattern.avgIntensities().get(DayOfWeek.MONDAY));
    }

    @Test
    void testOptimalContactWindow_immutable() {
        EmotionHistoryService.OptimalContactWindow window =
                new EmotionHistoryService.OptimalContactWindow(
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        0.8f,
                        Mood.HAPPY,
                        "test reason"
                );

        assertEquals(DayOfWeek.MONDAY, window.dayOfWeek());
        assertEquals(LocalTime.of(9, 0), window.startTime());
        assertEquals(LocalTime.of(10, 0), window.endTime());
        assertEquals(0.8f, window.score());
        assertEquals(Mood.HAPPY, window.expectedMood());
        assertEquals("test reason", window.reason());
    }

    @Test
    void testWeeklyContactAdvice_immutable() {
        EmotionHistoryService.WeeklyContactAdvice advice =
                new EmotionHistoryService.WeeklyContactAdvice(
                        List.of(),
                        List.of(DayOfWeek.SUNDAY),
                        "test summary",
                        10
                );

        assertTrue(advice.bestWindows().isEmpty());
        assertEquals(DayOfWeek.SUNDAY, advice.avoidDays().get(0));
        assertEquals("test summary", advice.summary());
        assertEquals(10, advice.dataPointsAnalyzed());
    }

    @Test
    void testTimePatternPrediction_immutable() {
        LocalDate date = LocalDate.now();
        EmotionHistoryService.TimePatternPrediction prediction =
                new EmotionHistoryService.TimePatternPrediction(
                        date, 10, Mood.HAPPY, 0.8f, 0.7f, "test basis"
                );

        assertEquals(date, prediction.date());
        assertEquals(10, prediction.hour());
        assertEquals(Mood.HAPPY, prediction.predictedMood());
        assertEquals(0.8f, prediction.confidence());
        assertEquals(0.7f, prediction.contactScore());
        assertEquals("test basis", prediction.basis());
    }

    @Test
    void testEmotionTrend_immutable() {
        EmotionHistoryService.EmotionTrend trend =
                new EmotionHistoryService.EmotionTrend(
                        "improving", 0.15f, 10, "test summary"
                );

        assertEquals("improving", trend.direction());
        assertEquals(0.15f, trend.changeRate());
        assertEquals(10, trend.dataPoints());
        assertEquals("test summary", trend.summary());
    }
}
