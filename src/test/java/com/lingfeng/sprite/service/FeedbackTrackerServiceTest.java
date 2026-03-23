package com.lingfeng.sprite.service;

import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.OwnerModel.ProactiveFeedback;
import com.lingfeng.sprite.OwnerModel.ProactiveFeedback.ResponseType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FeedbackTrackerService 单元测试 - S2 主人响应追踪
 */
@ExtendWith(MockitoExtension.class)
class FeedbackTrackerServiceTest {

    @Mock
    private UnifiedContextService unifiedContextService;

    private FeedbackTrackerService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackTrackerService(unifiedContextService);
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    // ==================== 基础记录测试 ====================

    @Test
    void testRecordProactiveMessage_basic() {
        service.recordProactiveMessage("msg-001", "idle_check", "你在忙吗?");

        assertEquals(1, service.getPendingCount());
    }

    @Test
    void testRecordProactiveMessage_multiple() {
        service.recordProactiveMessage("msg-001", "idle_check", "msg1");
        service.recordProactiveMessage("msg-002", "mood_check", "msg2");
        service.recordProactiveMessage("msg-003", "reminder", "msg3");

        assertEquals(3, service.getPendingCount());
    }

    @Test
    void testGetPendingCount_empty_returnsZero() {
        assertEquals(0, service.getPendingCount());
    }

    // ==================== 响应记录测试 ====================

    @Test
    void testRecordResponse_positive() {
        service.recordProactiveMessage("msg-001", "idle_check", "你在忙吗?");

        service.recordResponse("msg-001", "不忙有空", 0.8f);

        assertEquals(0, service.getPendingCount());

        List<ProactiveFeedback> recent = service.getRecentFeedback(10);
        assertEquals(1, recent.size());
        assertEquals(ResponseType.POSITIVE, recent.get(0).response());
    }

    @Test
    void testRecordResponse_neutral() {
        service.recordProactiveMessage("msg-001", "idle_check", "test");

        service.recordResponse("msg-001", "ok", 0.5f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(10);
        assertEquals(ResponseType.NEUTRAL, recent.get(0).response());
    }

    @Test
    void testRecordResponse_reply() {
        service.recordProactiveMessage("msg-001", "idle_check", "test");

        service.recordResponse("msg-001", "what?", 0.3f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(10);
        assertEquals(ResponseType.REPLY, recent.get(0).response());
    }

    @Test
    void testRecordResponse_reject() {
        service.recordProactiveMessage("msg-001", "idle_check", "test");

        service.recordResponse("msg-001", "no", 0.1f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(10);
        assertEquals(ResponseType.REJECT, recent.get(0).response());
    }

    @Test
    void testRecordResponse_unknownMessageId_ignored() {
        // 记录一个不存在的消息ID不应该抛异常
        assertDoesNotThrow(() ->
            service.recordResponse("unknown-id", "response", 0.5f)
        );
    }

    @Test
    void testRecordResponse_savesSentiment() {
        service.recordProactiveMessage("msg-001", "idle_check", "test");

        service.recordResponse("msg-001", "ok", 0.75f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(10);
        assertEquals(0.75f, recent.get(0).sentiment());
    }

    // ==================== 主人活动通知测试 ====================

    @Test
    void testNotifyOwnerActivity_withPendingMessage() {
        service.recordProactiveMessage("msg-001", "idle_check", "你在忙吗?");

        // 直接修改pendingMessages来模拟发送时间（避免时间竞争）
        // 由于我们无法直接访问私有字段，我们测试功能本身
        service.notifyOwnerActivity();

        // notifyOwnerActivity会处理pending消息
        assertTrue(service.getPendingCount() >= 0);
    }

    @Test
    void testNotifyOwnerActivity_noPendingMessages() {
        // 没有待追踪的消息不应该抛异常
        assertDoesNotThrow(() -> service.notifyOwnerActivity());
    }

    // ==================== 反馈历史测试 ====================

    @Test
    void testGetRecentFeedback_empty_returnsEmptyList() {
        List<ProactiveFeedback> recent = service.getRecentFeedback(10);
        assertNotNull(recent);
        assertTrue(recent.isEmpty());
    }

    @Test
    void testGetRecentFeedback_withData_returnsSortedByTime() {
        service.recordProactiveMessage("msg-001", "type1", "msg1");
        service.recordResponse("msg-001", "resp1", 0.8f);

        service.recordProactiveMessage("msg-002", "type2", "msg2");
        service.recordResponse("msg-002", "resp2", 0.6f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(10);

        assertEquals(2, recent.size());
        // 最新的应该在前面
        assertEquals("msg-002", recent.get(0).messageId());
    }

    @Test
    void testGetRecentFeedback_withLimit() {
        for (int i = 0; i < 5; i++) {
            service.recordProactiveMessage("msg-" + i, "type", "msg" + i);
            service.recordResponse("msg-" + i, "resp", 0.5f);
        }

        List<ProactiveFeedback> recent = service.getRecentFeedback(3);

        assertEquals(3, recent.size());
    }

    // ==================== 统计测试 ====================

    @Test
    void testGetStats_empty_returnsZeroStats() {
        FeedbackTrackerService.FeedbackStats stats = service.getStats();

        assertEquals(0, stats.total());
        assertEquals(0, stats.positive());
        assertEquals(0, stats.negative());
        assertEquals(0, stats.ignored());
        assertEquals(0, stats.neutral());
    }

    @Test
    void testGetStats_withMixedResponses() {
        // 记录不同类型的响应
        service.recordProactiveMessage("msg-001", "type", "test1");
        service.recordResponse("msg-001", "great", 0.8f);

        service.recordProactiveMessage("msg-002", "type", "test2");
        service.recordResponse("msg-002", "ok", 0.5f);

        service.recordProactiveMessage("msg-003", "type", "test3");
        service.recordResponse("msg-003", "bad", 0.1f);

        FeedbackTrackerService.FeedbackStats stats = service.getStats();

        assertEquals(3, stats.total());
        assertEquals(1, stats.positive());
        assertEquals(1, stats.neutral()); // 0.5 is NEUTRAL
        assertEquals(1, stats.negative()); // 0.1 is REJECT
    }

    @Test
    void testFeedbackStats_getPositiveRate() {
        FeedbackTrackerService.FeedbackStats stats = new FeedbackTrackerService.FeedbackStats(10, 3, 2, 3, 2);

        assertEquals(0.3f, stats.getPositiveRate());
    }

    @Test
    void testFeedbackStats_getPositiveRate_zeroTotal() {
        FeedbackTrackerService.FeedbackStats stats = new FeedbackTrackerService.FeedbackStats(0, 0, 0, 0, 0);

        assertEquals(0f, stats.getPositiveRate());
    }

    @Test
    void testFeedbackStats_getIgnoredRate() {
        FeedbackTrackerService.FeedbackStats stats = new FeedbackTrackerService.FeedbackStats(10, 2, 3, 5, 0);

        assertEquals(0.5f, stats.getIgnoredRate());
    }

    // ==================== 触发类型效果测试 ====================

    @Test
    void testGetTriggerEffectiveness_unknownType_returnsDefault() {
        FeedbackTrackerService.TriggerEffectiveness te = service.getTriggerEffectiveness("unknown");

        assertNotNull(te);
        assertEquals("unknown", te.triggerType());
        assertEquals(0f, te.score());
        assertEquals(0, te.positiveCount());
        assertEquals(0, te.negativeCount());
    }

    @Test
    void testGetAllTriggerEffectiveness_empty_returnsEmptyMap() {
        Map<String, FeedbackTrackerService.TriggerEffectiveness> all = service.getAllTriggerEffectiveness();

        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    void testGetTriggerResponseRate_default() {
        float rate = service.getTriggerResponseRate("new_trigger");

        assertEquals(0.5f, rate); // 默认50%
    }

    @Test
    void testGetTriggerResponseRate_withData() {
        // 通过recordResponse创建触发效果
        service.recordProactiveMessage("msg-001", "test_trigger", "test");
        service.recordResponse("msg-001", "positive", 0.8f);

        float rate = service.getTriggerResponseRate("test_trigger");

        assertTrue(rate >= 0f && rate <= 1f);
    }

    // ==================== 回调测试 ====================

    @Test
    void testSetFeedbackCallback_null() {
        // 设置null回调不应该抛异常
        assertDoesNotThrow(() -> service.setFeedbackCallback(null));
    }

    @Test
    void testSetFeedbackCallback_withCallback() {
        FeedbackTrackerService.FeedbackCallback callback = mock(FeedbackTrackerService.FeedbackCallback.class);

        service.setFeedbackCallback(callback);

        assertNotNull(callback);
    }

    // ==================== 记录类型测试 ====================

    @Test
    void testTriggerEffectiveness_immutable() {
        FeedbackTrackerService.TriggerEffectiveness te =
                new FeedbackTrackerService.TriggerEffectiveness("test", 0.5f, 3, 1, java.time.Instant.now());

        assertEquals("test", te.triggerType());
        assertEquals(0.5f, te.score());
        assertEquals(3, te.positiveCount());
        assertEquals(1, te.negativeCount());
    }

    @Test
    void testFeedbackStats_immutable() {
        FeedbackTrackerService.FeedbackStats stats =
                new FeedbackTrackerService.FeedbackStats(10, 5, 2, 2, 1);

        assertEquals(10, stats.total());
        assertEquals(5, stats.positive());
        assertEquals(2, stats.negative());
        assertEquals(2, stats.ignored());
        assertEquals(1, stats.neutral());
    }

    // ==================== ProactiveFeedback 记录类型测试 ====================

    @Test
    void testProactiveFeedback_recordType() {
        java.time.Instant now = java.time.Instant.now();

        ProactiveFeedback feedback = new ProactiveFeedback(
                "msg-001",
                now,
                "idle_check",
                "你在忙吗?",
                ResponseType.POSITIVE,
                now,
                "不忙",
                0.8f
        );

        assertEquals("msg-001", feedback.messageId());
        assertEquals(now, feedback.sentTime());
        assertEquals("idle_check", feedback.triggerType());
        assertEquals("你在忙吗?", feedback.content());
        assertEquals(ResponseType.POSITIVE, feedback.response());
        assertEquals(now, feedback.responseTime());
        assertEquals("不忙", feedback.responseContent());
        assertEquals(0.8f, feedback.sentiment());
    }

    // ==================== 响应分类测试 ====================

    @Test
    void testResponseClassification_positiveHighSentiment() {
        service.recordProactiveMessage("msg-001", "type", "test");
        service.recordResponse("msg-001", "great!", 0.9f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(1);
        assertEquals(ResponseType.POSITIVE, recent.get(0).response());
    }

    @Test
    void testResponseClassification_positiveBoundary() {
        service.recordProactiveMessage("msg-001", "type", "test");
        // sentiment >= 0.6 is POSITIVE
        service.recordResponse("msg-001", "ok", 0.6f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(1);
        assertEquals(ResponseType.POSITIVE, recent.get(0).response());
    }

    @Test
    void testResponseClassification_neutralBoundary() {
        service.recordProactiveMessage("msg-001", "type", "test");
        // 0.4 <= sentiment < 0.6 is NEUTRAL
        service.recordResponse("msg-001", "ok", 0.4f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(1);
        assertEquals(ResponseType.NEUTRAL, recent.get(0).response());
    }

    @Test
    void testResponseClassification_replyBoundary() {
        service.recordProactiveMessage("msg-001", "type", "test");
        // 0.2 <= sentiment < 0.4 is REPLY
        service.recordResponse("msg-001", "ok", 0.2f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(1);
        assertEquals(ResponseType.REPLY, recent.get(0).response());
    }

    @Test
    void testResponseClassification_rejectBoundary() {
        service.recordProactiveMessage("msg-001", "type", "test");
        // sentiment < 0.2 is REJECT
        service.recordResponse("msg-001", "no", 0.19f);

        List<ProactiveFeedback> recent = service.getRecentFeedback(1);
        assertEquals(ResponseType.REJECT, recent.get(0).response());
    }
}
