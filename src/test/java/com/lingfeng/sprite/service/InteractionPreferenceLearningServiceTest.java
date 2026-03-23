package com.lingfeng.sprite.service;

import com.lingfeng.sprite.OwnerModel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * InteractionPreferenceLearningService 单元测试 - S2 交互偏好学习
 */
@ExtendWith(MockitoExtension.class)
class InteractionPreferenceLearningServiceTest {

    @Mock
    private UnifiedContextService unifiedContextService;

    @Mock
    private FeedbackTrackerService feedbackTrackerService;

    private InteractionPreferenceLearningService service;

    @BeforeEach
    void setUp() {
        // 不启动scheduler来避免定时任务
        // 直接测试核心方法
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    // ==================== InteractionPreferences 记录测试 ====================

    @Test
    void testInteractionPreferences_defaultValues() {
        InteractionPreferenceLearningService.InteractionPreferences prefs =
                new InteractionPreferenceLearningService.InteractionPreferences(
                        null,  // verbosity
                        null,  // tone
                        null,  // bestContactHours
                        null,  // bestContactDays
                        null,  // triggerResponseRates
                        10,   // totalInteractions
                        0.7f, // avgResponseRate
                        5,    // activeDays
                        0.8f, // verbosityConfidence
                        0.6f, // toneConfidence
                        0.5f  // bestTimeConfidence
                );

        // 验证默认值
        assertEquals(Verbosity.MODERATE, prefs.inferredVerbosity());
        assertEquals("友好", prefs.inferredTone());
        assertTrue(prefs.bestContactHours().isEmpty());
        assertTrue(prefs.bestContactDays().isEmpty());
        assertTrue(prefs.triggerResponseRates().isEmpty());
    }

    @Test
    void testInteractionPreferences_withValues() {
        List<Integer> hours = List.of(9, 10, 11);
        List<DayOfWeek> days = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        Map<String, Float> rates = Map.of("idle_check", 0.8f, "reminder", 0.6f);

        InteractionPreferenceLearningService.InteractionPreferences prefs =
                new InteractionPreferenceLearningService.InteractionPreferences(
                        Verbosity.BRIEF,
                        "热情",
                        hours,
                        days,
                        rates,
                        20,
                        0.75f,
                        10,
                        0.9f,
                        0.7f,
                        0.6f
                );

        assertEquals(Verbosity.BRIEF, prefs.inferredVerbosity());
        assertEquals("热情", prefs.inferredTone());
        assertEquals(3, prefs.bestContactHours().size());
        assertEquals(2, prefs.bestContactDays().size());
        assertEquals(0.8f, prefs.triggerResponseRates().get("idle_check"));
    }

    // ==================== Verbosity 枚举测试 ====================

    @Test
    void testVerbosityValues() {
        Verbosity[] values = Verbosity.values();
        assertEquals(3, values.length);
        assertNotNull(Verbosity.BRIEF);
        assertNotNull(Verbosity.MODERATE);
        assertNotNull(Verbosity.DETAILED);
    }

    // ==================== InteractionType 枚举测试 ====================

    @Test
    void testInteractionTypeValues() {
        InteractionType[] values = InteractionType.values();
        assertTrue(values.length > 0);
        assertNotNull(InteractionType.CHAT);
        assertNotNull(InteractionType.COMMAND);
    }

    // ==================== ResponseType 枚举测试 ====================

    @Test
    void testResponseTypeValues() {
        ProactiveFeedback.ResponseType[] values = ProactiveFeedback.ResponseType.values();
        assertEquals(5, values.length);
        assertNotNull(ProactiveFeedback.ResponseType.POSITIVE);
        assertNotNull(ProactiveFeedback.ResponseType.NEUTRAL);
        assertNotNull(ProactiveFeedback.ResponseType.REPLY);
        assertNotNull(ProactiveFeedback.ResponseType.REJECT);
        assertNotNull(ProactiveFeedback.ResponseType.IGNORE);
    }

    // ==================== Interaction 记录测试 ====================

    @Test
    void testInteractionRecord() {
        Instant now = Instant.now();

        Interaction interaction = new Interaction(
                now,
                InteractionType.CHAT,
                "test message",
                0.8f,
                "idle_check",
                "response",
                null
        );

        assertEquals(now, interaction.timestamp());
        assertEquals(InteractionType.CHAT, interaction.type());
        assertEquals("test message", interaction.content());
        assertEquals(0.8f, interaction.sentiment());
        assertEquals("idle_check", interaction.triggerType());
        assertEquals("response", interaction.responseContent());
    }

    // ==================== ProactiveFeedback 记录测试 ====================

    @Test
    void testProactiveFeedbackRecord() {
        Instant now = Instant.now();

        ProactiveFeedback feedback = new ProactiveFeedback(
                "msg-001",
                now,
                "idle_check",
                "你在忙吗?",
                ProactiveFeedback.ResponseType.POSITIVE,
                now,
                "不忙",
                0.8f
        );

        assertEquals("msg-001", feedback.messageId());
        assertEquals(now, feedback.sentTime());
        assertEquals("idle_check", feedback.triggerType());
        assertEquals("你在忙吗?", feedback.content());
        assertEquals(ProactiveFeedback.ResponseType.POSITIVE, feedback.response());
        assertEquals(0.8f, feedback.sentiment());
    }

    // ==================== OwnerModel 记录测试 ====================

    @Test
    void testOwnerModelRecord() {
        OwnerModel owner = new OwnerModel(
                "test-owner",
                "测试主人",
                Map.of(),
                List.of(),
                new OwnerModel.InteractionPreferences(
                        Verbosity.BRIEF,
                        "热情",
                        List.of(9, 10),
                        List.of(DayOfWeek.MONDAY),
                        Map.of(),
                        10,
                        0.8f,
                        5
                )
        );

        assertEquals("test-owner", owner.id());
        assertEquals("测试主人", owner.name());
    }

    // ==================== 逻辑推断测试 ====================

    @Test
    void testConfidenceCalculation() {
        // 测试置信度计算公式: Math.min(1f, sampleCount / (minRequired * 3))
        // sampleCount = 0 -> 0
        assertEquals(0f, calculateConfidence(0, 5));
        // sampleCount = 5 -> 5/(5*3) = 0.33
        assertEquals(0.33f, calculateConfidence(5, 5), 0.01f);
        // sampleCount = 15 -> 15/(5*3) = 1.0
        assertEquals(1f, calculateConfidence(15, 5), 0.01f);
        // sampleCount = 20 -> capped at 1.0
        assertEquals(1f, calculateConfidence(20, 5), 0.01f);
    }

    @Test
    void testVerbosityInference_shortMessages() {
        // 平均长度 < 20 -> BRIEF
        Verbosity result = inferVerbosity(List.of(
            createInteraction("短"),
            createInteraction("简"),
            createInteraction("洁"),
            createInteraction("短"),
            createInteraction("短")
        ));
        assertEquals(Verbosity.BRIEF, result);
    }

    @Test
    void testVerbosityInference_longMessages() {
        // 平均长度 > 80 -> DETAILED
        Verbosity result = inferVerbosity(List.of(
            createInteraction("这是一条比较长的消息用来测试详细模式的判断标准是否正确"),
            createInteraction("这是第二条比较长的消息内容来测试是否会判断为详细模式"),
            createInteraction("这是第三条消息继续保持较长的内容来进行测试"),
            createInteraction("第四条消息仍然需要保持足够的长度来满足详细模式的要求"),
            createInteraction("第五条消息继续添加内容以确保平均长度超过阈值")
        ));
        assertEquals(Verbosity.DETAILED, result);
    }

    @Test
    void testVerbosityInference_moderateMessages() {
        // 20 <= 平均长度 <= 80 -> MODERATE
        Verbosity result = inferVerbosity(List.of(
            createInteraction("中等长度消息"),
            createInteraction("第二条消息"),
            createInteraction("第三条消息"),
            createInteraction("第四条"),
            createInteraction("第五条")
        ));
        assertEquals(Verbosity.MODERATE, result);
    }

    @Test
    void testVerbosityInference_insufficientSamples() {
        // 样本少于5 -> MODERATE
        Verbosity result = inferVerbosity(List.of(
            createInteraction("短"),
            createInteraction("短")
        ));
        assertEquals(Verbosity.MODERATE, result);
    }

    @Test
    void testToneInference_positiveSentiment() {
        // 平均情感 >= 0.6 -> 热情
        String tone = inferTone(List.of(
            createInteractionWithSentiment("消息1", 0.7f),
            createInteractionWithSentiment("消息2", 0.8f),
            createInteractionWithSentiment("消息3", 0.6f),
            createInteractionWithSentiment("消息4", 0.7f),
            createInteractionWithSentiment("消息5", 0.6f)
        ));
        assertEquals("热情", tone);
    }

    @Test
    void testToneInference_neutralSentiment() {
        // 0.4 <= 平均情感 < 0.6 -> 友好
        String tone = inferTone(List.of(
            createInteractionWithSentiment("消息1", 0.5f),
            createInteractionWithSentiment("消息2", 0.5f),
            createInteractionWithSentiment("消息3", 0.5f),
            createInteractionWithSentiment("消息4", 0.5f),
            createInteractionWithSentiment("消息5", 0.5f)
        ));
        assertEquals("友好", tone);
    }

    @Test
    void testToneInference_lowSentiment() {
        // 0.25 <= 平均情感 < 0.4 -> 中性
        String tone = inferTone(List.of(
            createInteractionWithSentiment("消息1", 0.3f),
            createInteractionWithSentiment("消息2", 0.3f),
            createInteractionWithSentiment("消息3", 0.3f),
            createInteractionWithSentiment("消息4", 0.3f),
            createInteractionWithSentiment("消息5", 0.3f)
        ));
        assertEquals("中性", tone);
    }

    @Test
    void testToneInference_veryLowSentiment() {
        // 平均情感 < 0.25 -> 冷淡
        String tone = inferTone(List.of(
            createInteractionWithSentiment("消息1", 0.1f),
            createInteractionWithSentiment("消息2", 0.15f),
            createInteractionWithSentiment("消息3", 0.2f),
            createInteractionWithSentiment("消息4", 0.1f),
            createInteractionWithSentiment("消息5", 0.15f)
        ));
        assertEquals("冷淡", tone);
    }

    @Test
    void testToneInference_insufficientSamples() {
        // 样本少于5 -> 友好
        String tone = inferTone(List.of(
            createInteractionWithSentiment("消息1", 0.8f),
            createInteractionWithSentiment("消息2", 0.1f)
        ));
        assertEquals("友好", tone);
    }

    @Test
    void testToneInference_noPositiveSentiment() {
        // 没有情感数据 -> 友好
        String tone = inferTone(List.of(
            new Interaction(Instant.now(), InteractionType.CHAT, "消息", 0, null, null, null),
            new Interaction(Instant.now(), InteractionType.CHAT, "消息", 0, null, null, null)
        ));
        assertEquals("友好", tone);
    }

    // ==================== 辅助方法 ====================

    private float calculateConfidence(int sampleCount, int minRequired) {
        if (sampleCount <= 0) return 0f;
        return Math.min(1f, (float) sampleCount / (minRequired * 3));
    }

    private Verbosity inferVerbosity(List<Interaction> interactions) {
        int MIN_SAMPLES = 5;
        if (interactions.size() < MIN_SAMPLES) {
            return Verbosity.MODERATE;
        }

        List<Integer> messageLengths = interactions.stream()
                .filter(i -> i.content() != null)
                .map(i -> i.content().length())
                .toList();

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

    private String inferTone(List<Interaction> interactions) {
        int MIN_SAMPLES = 5;
        if (interactions.size() < MIN_SAMPLES) {
            return "友好";
        }

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

    private Interaction createInteraction(String content) {
        return new Interaction(Instant.now(), InteractionType.CHAT, content, 0.5f, null, null, null);
    }

    private Interaction createInteractionWithSentiment(String content, float sentiment) {
        return new Interaction(Instant.now(), InteractionType.CHAT, content, sentiment, null, null, null);
    }
}
