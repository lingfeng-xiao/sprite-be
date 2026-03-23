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

    public SpriteController(
            SpriteService spriteService,
            HealthMonitorService healthMonitorService,
            FeedbackTrackerService feedbackTrackerService,
            InteractionPreferenceLearningService preferenceLearningService
    ) {
        this.spriteService = spriteService;
        this.healthMonitorService = healthMonitorService;
        this.feedbackTrackerService = feedbackTrackerService;
        this.preferenceLearningService = preferenceLearningService;
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
