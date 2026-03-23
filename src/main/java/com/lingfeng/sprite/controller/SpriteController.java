package com.lingfeng.sprite.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.Sprite;
import com.lingfeng.sprite.service.FeedbackTrackerService;
import com.lingfeng.sprite.service.HealthMonitorService;
import com.lingfeng.sprite.service.InteractionPreferenceLearningService;
import com.lingfeng.sprite.service.EmotionHistoryService;
import com.lingfeng.sprite.service.SpriteService;

/**
 * Sprite REST API 控制器
 *
 * 提供认知循环触发、状态查询、反馈提交等 REST API
 */
@RestController
@RequestMapping("/api/sprite")
public class SpriteController {

    private final SpriteService spriteService;
    private final HealthMonitorService healthMonitorService;
    private final FeedbackTrackerService feedbackTrackerService;
    private final InteractionPreferenceLearningService preferenceLearningService;
    private final EmotionHistoryService emotionHistoryService;

    public SpriteController(
            SpriteService spriteService,
            HealthMonitorService healthMonitorService,
            FeedbackTrackerService feedbackTrackerService,
            InteractionPreferenceLearningService preferenceLearningService,
            EmotionHistoryService emotionHistoryService
    ) {
        this.spriteService = spriteService;
        this.healthMonitorService = healthMonitorService;
        this.feedbackTrackerService = feedbackTrackerService;
        this.preferenceLearningService = preferenceLearningService;
        this.emotionHistoryService = emotionHistoryService;
    }

    /**
     * POST /api/sprite/cycle - 手动触发一轮认知闭环
     */
    @PostMapping("/cycle")
    public ResponseEntity<CognitionController.CognitionResult> cognitionCycle() {
        CognitionController.CognitionResult result = spriteService.cognitionCycle();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/sprite/state - 获取 Sprite 当前状态
     */
    @GetMapping("/state")
    public ResponseEntity<Sprite.State> getState() {
        Sprite.State state = spriteService.getState();
        return ResponseEntity.ok(state);
    }

    /**
     * POST /api/sprite/feedback - 提交反馈
     */
    @PostMapping("/feedback")
    public ResponseEntity<Void> recordFeedback(@RequestBody FeedbackRequest request) {
        spriteService.recordFeedback(
                request.type(),
                request.content(),
                request.outcome(),
                request.success(),
                request.impact()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/sprite/memory - 获取记忆系统状态
     */
    @GetMapping("/memory")
    public ResponseEntity<MemorySystem.MemoryStatus> getMemoryStatus() {
        MemorySystem.MemoryStatus status = spriteService.getMemoryStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/sprite/evolution - 获取进化状态
     */
    @GetMapping("/evolution")
    public ResponseEntity<EvolutionEngine.EvolutionStatus> getEvolutionStatus() {
        EvolutionEngine.EvolutionStatus status = spriteService.getEvolutionStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/sprite/stats - 获取认知统计
     */
    @GetMapping("/stats")
    public ResponseEntity<CognitionController.CognitionStats> getCognitionStats() {
        CognitionController.CognitionStats stats = spriteService.getCognitionStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/sprite/health - 获取系统健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<HealthMonitorService.HealthDetails> getHealth() {
        HealthMonitorService.HealthDetails health = healthMonitorService.getHealthDetails();
        return ResponseEntity.ok(health);
    }

    /**
     * GET /api/sprite/feedback - 获取主人反馈统计
     */
    @GetMapping("/feedback")
    public ResponseEntity<FeedbackTrackerService.FeedbackStats> getFeedbackStats() {
        FeedbackTrackerService.FeedbackStats stats = feedbackTrackerService.getStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/sprite/preferences - 获取主人交互偏好
     */
    @GetMapping("/preferences")
    public ResponseEntity<InteractionPreferenceLearningService.InteractionPreferences> getPreferences() {
        InteractionPreferenceLearningService.InteractionPreferences prefs =
                preferenceLearningService.getPreferences();
        return ResponseEntity.ok(prefs);
    }

    /**
     * GET /api/sprite/emotions - 获取主人情绪历史
     */
    @GetMapping("/emotions")
    public ResponseEntity<EmotionHistoryService.EmotionStats> getEmotionStats() {
        EmotionHistoryService.EmotionRecord current = emotionHistoryService.getCurrentEmotion();
        if (current == null) {
            return ResponseEntity.noContent().build();
        }
        // 获取今天的统计
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        EmotionHistoryService.EmotionStats stats = emotionHistoryService.getStatsForDate(today);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/sprite/emotions/weekly - 获取周情绪模式
     */
    @GetMapping("/emotions/weekly")
    public ResponseEntity<EmotionHistoryService.WeeklyPattern> getWeeklyEmotionPattern() {
        EmotionHistoryService.WeeklyPattern pattern = emotionHistoryService.getWeeklyPattern();
        return ResponseEntity.ok(pattern);
    }

    /**
     * S3-2: GET /api/sprite/emotions/contact-advice - 获取每周联系建议
     */
    @GetMapping("/emotions/contact-advice")
    public ResponseEntity<EmotionHistoryService.WeeklyContactAdvice> getContactAdvice() {
        EmotionHistoryService.WeeklyContactAdvice advice = emotionHistoryService.getWeeklyContactAdvice();
        return ResponseEntity.ok(advice);
    }

    /**
     * S3-2: GET /api/sprite/emotions/optimal-windows - 获取最优联系时间窗口
     */
    @GetMapping("/emotions/optimal-windows")
    public ResponseEntity<List<EmotionHistoryService.OptimalContactWindow>> getOptimalContactWindows() {
        List<EmotionHistoryService.OptimalContactWindow> windows =
                emotionHistoryService.getOptimalContactWindows();
        return ResponseEntity.ok(windows);
    }

    /**
     * S3-3: GET /api/sprite/emotions/predict - 预测指定时间的情绪
     */
    @GetMapping("/emotions/predict")
    public ResponseEntity<EmotionHistoryService.TimePatternPrediction> predictEmotion(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "12") int hour) {
        java.time.LocalDate targetDate;
        if (date != null && !date.isEmpty()) {
            targetDate = java.time.LocalDate.parse(date);
        } else {
            targetDate = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        }
        EmotionHistoryService.TimePatternPrediction prediction =
                emotionHistoryService.predictEmotion(targetDate, hour);
        return ResponseEntity.ok(prediction);
    }

    /**
     * S3-3: GET /api/sprite/emotions/trend - 获取情绪趋势
     */
    @GetMapping("/emotions/trend")
    public ResponseEntity<EmotionHistoryService.EmotionTrend> getEmotionTrend(
            @RequestParam(defaultValue = "7") int days) {
        EmotionHistoryService.EmotionTrend trend = emotionHistoryService.getEmotionTrend(days);
        return ResponseEntity.ok(trend);
    }

    /**
     * POST /api/sprite/start - 启动 Sprite
     */
    @PostMapping("/start")
    public ResponseEntity<Void> start() {
        spriteService.start();
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/sprite/stop - 停止 Sprite
     */
    @PostMapping("/stop")
    public ResponseEntity<Void> stop() {
        spriteService.stop();
        return ResponseEntity.ok().build();
    }

    /**
     * 反馈请求 DTO
     */
    public record FeedbackRequest(
            EvolutionEngine.Feedback.FeedbackSource type,
            String content,
            String outcome,
            boolean success,
            EvolutionEngine.Impact impact
    ) {}
}
