package com.lingfeng.sprite.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.Sprite;
import com.lingfeng.sprite.service.FeedbackTrackerService;
import com.lingfeng.sprite.service.GitHubBackupService;
import com.lingfeng.sprite.service.HealthMonitorService;
import com.lingfeng.sprite.service.InteractionPreferenceLearningService;
import com.lingfeng.sprite.service.EmotionHistoryService;
import com.lingfeng.sprite.service.MemoryVisualizationService;
import com.lingfeng.sprite.service.OwnerEmotionDashboardService;
import com.lingfeng.sprite.service.EvolutionDashboardService;
import com.lingfeng.sprite.service.CognitionDashboardService;
import com.lingfeng.sprite.service.ExternalApiAdapterService;
import com.lingfeng.sprite.service.PerformanceMonitorService;
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
    private final GitHubBackupService gitHubBackupService;
    private final MemoryVisualizationService memoryVisualizationService;
    private final OwnerEmotionDashboardService emotionDashboardService;
    private final EvolutionDashboardService evolutionDashboardService;
    private final CognitionDashboardService cognitionDashboardService;
    private final ExternalApiAdapterService externalApiService;
    private final PerformanceMonitorService performanceMonitorService;

    public SpriteController(
            SpriteService spriteService,
            HealthMonitorService healthMonitorService,
            FeedbackTrackerService feedbackTrackerService,
            InteractionPreferenceLearningService preferenceLearningService,
            EmotionHistoryService emotionHistoryService,
            GitHubBackupService gitHubBackupService,
            MemoryVisualizationService memoryVisualizationService,
            OwnerEmotionDashboardService emotionDashboardService,
            EvolutionDashboardService evolutionDashboardService,
            CognitionDashboardService cognitionDashboardService,
            ExternalApiAdapterService externalApiService,
            PerformanceMonitorService performanceMonitorService
    ) {
        this.spriteService = spriteService;
        this.healthMonitorService = healthMonitorService;
        this.feedbackTrackerService = feedbackTrackerService;
        this.preferenceLearningService = preferenceLearningService;
        this.emotionHistoryService = emotionHistoryService;
        this.gitHubBackupService = gitHubBackupService;
        this.memoryVisualizationService = memoryVisualizationService;
        this.emotionDashboardService = emotionDashboardService;
        this.evolutionDashboardService = evolutionDashboardService;
        this.cognitionDashboardService = cognitionDashboardService;
        this.externalApiService = externalApiService;
        this.performanceMonitorService = performanceMonitorService;
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
     * S12-1: GET /api/sprite/memory/visualization - 获取记忆可视化数据（连接真实MemorySystem）
     */
    @GetMapping("/memory/visualization")
    public ResponseEntity<MemoryVisualizationService.MemoryVisualizationData> getMemoryVisualization() {
        MemorySystem.Memory memory = spriteService.getMemory();
        MemoryVisualizationService.MemoryVisualizationData data =
                memoryVisualizationService.generateVisualization(memory);
        return ResponseEntity.ok(data);
    }

    /**
     * S12-1: GET /api/sprite/memory/timeline - 获取记忆时间线（连接真实MemorySystem）
     * @param startDate ISO格式开始日期（可选，默认30天前）
     * @param endDate ISO格式结束日期（可选，默认当前时间）
     */
    @GetMapping("/memory/timeline")
    public ResponseEntity<MemoryVisualizationService.MemoryTimeline> getMemoryTimeline(
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate
    ) {
        MemorySystem.Memory memory = spriteService.getMemory();
        MemoryVisualizationService.MemoryTimeline timeline =
                memoryVisualizationService.generateTimeline(memory, startDate, endDate);
        return ResponseEntity.ok(timeline);
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
     * S12-3: GET /api/sprite/evolution/dashboard - 获取进化Dashboard数据（连接真实EvolutionEngine）
     */
    @GetMapping("/evolution/dashboard")
    public ResponseEntity<EvolutionDashboardService.EvolutionDashboardData> getEvolutionDashboard() {
        EvolutionDashboardService.EvolutionDashboardData data =
                evolutionDashboardService.getDashboardData(spriteService.getEvolutionEngine());
        return ResponseEntity.ok(data);
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
     * S12-4: GET /api/sprite/cognition/dashboard - 获取认知Dashboard数据
     */
    @GetMapping("/cognition/dashboard")
    public ResponseEntity<CognitionDashboardService.CognitionDashboardData> getCognitionDashboard() {
        CognitionDashboardService.CognitionDashboardData data =
                cognitionDashboardService.getDashboardData();
        return ResponseEntity.ok(data);
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
     * S12-2: GET /api/sprite/emotions/dashboard - 获取主人情绪Dashboard聚合数据（连接真实EmotionHistoryService）
     */
    @GetMapping("/emotions/dashboard")
    public ResponseEntity<OwnerEmotionDashboardService.OwnerEmotionDashboardData> getEmotionDashboard() {
        OwnerEmotionDashboardService.OwnerEmotionDashboardData data =
                emotionDashboardService.generateDashboardData(emotionHistoryService);
        return ResponseEntity.ok(data);
    }

    // ==================== S4: GitHub备份接口 ====================

    /**
     * S4-1: POST /api/sprite/backup - 手动触发GitHub备份
     */
    @PostMapping("/backup")
    public ResponseEntity<GitHubBackupService.BackupResult> triggerBackup() {
        GitHubBackupService.BackupResult result = gitHubBackupService.forceBackup();
        return ResponseEntity.ok(result);
    }

    /**
     * S4-1: GET /api/sprite/backup/index - 获取备份索引
     */
    @GetMapping("/backup/index")
    public ResponseEntity<GitHubBackupService.BackupIndex> getBackupIndex() {
        try {
            GitHubBackupService.BackupIndex index = gitHubBackupService.getBackupIndex();
            return ResponseEntity.ok(index);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * S4-1: GET /api/sprite/backup/snapshot - 获取指定版本的记忆快照
     */
    @GetMapping("/backup/snapshot")
    public ResponseEntity<GitHubBackupService.MemorySnapshot> getMemorySnapshot(
            @RequestParam String timestamp) {
        GitHubBackupService.MemorySnapshot snapshot = gitHubBackupService.getMemorySnapshot(timestamp);
        return ResponseEntity.ok(snapshot);
    }

    /**
     * S4-1: GET /api/sprite/backup/status - 获取备份状态
     */
    @GetMapping("/backup/status")
    public ResponseEntity<BackupStatus> getBackupStatus() {
        java.time.Instant lastBackup = gitHubBackupService.getLastBackupTime();
        return ResponseEntity.ok(new BackupStatus(
                lastBackup != null,
                lastBackup != null ? lastBackup.toString() : null
        ));
    }

    /**
     * S4-2: GET /api/sprite/backup/list - 获取可用备份列表
     */
    @GetMapping("/backup/list")
    public ResponseEntity<GitHubBackupService.BackupListResult> listBackups() {
        GitHubBackupService.BackupListResult result = gitHubBackupService.listBackups();
        return ResponseEntity.ok(result);
    }

    /**
     * S4-2: POST /api/sprite/backup/restore - 从备份恢复
     */
    @PostMapping("/backup/restore")
    public ResponseEntity<GitHubBackupService.RestoreResult> restoreFromBackup(
            @RequestParam String timestamp) {
        GitHubBackupService.RestoreResult result = gitHubBackupService.restoreFromBackup(timestamp);
        return ResponseEntity.ok(result);
    }

    /**
     * S4-2: GET /api/sprite/backup/compare - 比较两个备份版本
     */
    @GetMapping("/backup/compare")
    public ResponseEntity<GitHubBackupService.DiffResult> compareBackups(
            @RequestParam String timestamp1,
            @RequestParam String timestamp2) {
        GitHubBackupService.DiffResult result = gitHubBackupService.compareBackups(timestamp1, timestamp2);
        return ResponseEntity.ok(result);
    }

    /**
     * S4-3: GET /api/sprite/backup/conflicts - 检测冲突
     */
    @GetMapping("/backup/conflicts")
    public ResponseEntity<GitHubBackupService.ConflictCheckResult> checkConflicts() {
        GitHubBackupService.ConflictCheckResult result = gitHubBackupService.checkConflicts();
        return ResponseEntity.ok(result);
    }

    /**
     * 备份状态
     */
    public record BackupStatus(
            boolean backupEnabled,
            String lastBackupTime
    ) {}

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

    // ==================== S13-2: 外部API接口 ====================

    /**
     * S13-2: GET /api/sprite/external/weather - 查询天气
     */
    @GetMapping("/external/weather")
    public ResponseEntity<ExternalApiAdapterService.ApiResponse> getWeather(
            @RequestParam String city) {
        ExternalApiAdapterService.ApiResponse response = externalApiService.getWeather(city);
        return ResponseEntity.ok(response);
    }

    /**
     * S13-2: GET /api/sprite/external/news - 查询新闻
     */
    @GetMapping("/external/news")
    public ResponseEntity<ExternalApiAdapterService.ApiResponse> getNews(
            @RequestParam(defaultValue = "general") String topic,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        ExternalApiAdapterService.ApiResponse response = externalApiService.getNews(topic, page, pageSize);
        return ResponseEntity.ok(response);
    }

    /**
     * S13-2: GET /api/sprite/external/search - 网络搜索
     */
    @GetMapping("/external/search")
    public ResponseEntity<ExternalApiAdapterService.ApiResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int numResults) {
        ExternalApiAdapterService.ApiResponse response = externalApiService.search(query, numResults);
        return ResponseEntity.ok(response);
    }

    /**
     * S13-2: GET /api/sprite/external/translate - 翻译
     */
    @GetMapping("/external/translate")
    public ResponseEntity<ExternalApiAdapterService.ApiResponse> translate(
            @RequestParam String text,
            @RequestParam(defaultValue = "auto") String from,
            @RequestParam(defaultValue = "zh") String to) {
        ExternalApiAdapterService.ApiResponse response = externalApiService.translate(text, from, to);
        return ResponseEntity.ok(response);
    }

    // ==================== S13-4: 性能监控接口 ====================

    /**
     * S13-4: GET /api/sprite/monitor/alerts - 检查性能告警并触发Webhook
     */
    @GetMapping("/monitor/alerts")
    public ResponseEntity<List<PerformanceMonitorService.Alert>> checkAlerts() {
        List<PerformanceMonitorService.Alert> alerts = performanceMonitorService.checkAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * S13-4: GET /api/sprite/monitor/snapshot - 获取性能快照
     */
    @GetMapping("/monitor/snapshot")
    public ResponseEntity<PerformanceMonitorService.PerformanceSnapshot> getSnapshot() {
        PerformanceMonitorService.PerformanceSnapshot snapshot = performanceMonitorService.getSnapshot();
        return ResponseEntity.ok(snapshot);
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
