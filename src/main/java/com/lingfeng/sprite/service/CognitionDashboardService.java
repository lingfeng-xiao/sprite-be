package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S10-1: 认知Dashboard服务
 *
 * 提供认知状态的可视化数据：
 * - 认知循环状态
 * - 推理引擎状态
 * - 决策过程追踪
 * - 记忆检索统计
 */
public class CognitionDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(CognitionDashboardService.class);

    private final List<CognitionEvent> eventHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 200;

    /**
     * 认知事件
     */
    public record CognitionEvent(
        Instant timestamp,
        CognitionPhase phase,
        String description,
        float durationMs,
        boolean success
    ) {}

    /**
     * 认知阶段
     */
    public enum CognitionPhase {
        PERCEPTION,    // 感知
        CONTEXT_BUILD, // 上下文构建
        REASONING,     // 推理
        DECISION,      // 决策
        ACTION,        // 动作
        LEARNING       // 学习
    }

    public CognitionDashboardService() {}

    // ==================== 事件记录 ====================

    /**
     * 记录认知事件
     */
    public void recordEvent(CognitionPhase phase, String description, float durationMs, boolean success) {
        CognitionEvent event = new CognitionEvent(
            Instant.now(),
            phase,
            description,
            durationMs,
            success
        );
        eventHistory.add(event);

        // 清理过期事件
        while (eventHistory.size() > MAX_HISTORY) {
            eventHistory.remove(0);
        }
    }

    /**
     * 记录感知阶段
     */
    public void recordPerception(String description, float durationMs, boolean success) {
        recordEvent(CognitionPhase.PERCEPTION, description, durationMs, success);
    }

    /**
     * 记录上下文构建阶段
     */
    public void recordContextBuilding(String description, float durationMs, boolean success) {
        recordEvent(CognitionPhase.CONTEXT_BUILD, description, durationMs, success);
    }

    /**
     * 记录推理阶段
     */
    public void recordReasoning(String description, float durationMs, boolean success) {
        recordEvent(CognitionPhase.REASONING, description, durationMs, success);
    }

    /**
     * 记录决策阶段
     */
    public void recordDecision(String description, float durationMs, boolean success) {
        recordEvent(CognitionPhase.DECISION, description, durationMs, success);
    }

    /**
     * 记录动作阶段
     */
    public void recordAction(String description, float durationMs, boolean success) {
        recordEvent(CognitionPhase.ACTION, description, durationMs, success);
    }

    /**
     * 记录学习阶段
     */
    public void recordLearning(String description, float durationMs, boolean success) {
        recordEvent(CognitionPhase.LEARNING, description, durationMs, success);
    }

    // ==================== 数据获取 ====================

    /**
     * 获取最近事件
     */
    public List<CognitionEvent> getRecentEvents(int count) {
        if (eventHistory.isEmpty()) return List.of();
        int size = Math.min(count, eventHistory.size());
        return eventHistory.subList(eventHistory.size() - size, eventHistory.size());
    }

    /**
     * 获取所有事件
     */
    public List<CognitionEvent> getAllEvents() {
        return new ArrayList<>(eventHistory);
    }

    /**
     * 获取指定阶段的事件
     */
    public List<CognitionEvent> getEventsByPhase(CognitionPhase phase) {
        return eventHistory.stream()
            .filter(e -> e.phase() == phase)
            .toList();
    }

    // ==================== 统计和摘要 ====================

    /**
     * 获取认知Dashboard数据
     */
    public CognitionDashboardData getDashboardData() {
        Instant now = Instant.now();
        Instant oneHourAgo = now.minusSeconds(3600);

        List<CognitionEvent> recentEvents = eventHistory.stream()
            .filter(e -> e.timestamp().isAfter(oneHourAgo))
            .toList();

        // 按阶段统计
        PhaseStats[] phaseStats = new PhaseStats[CognitionPhase.values().length];
        for (int i = 0; i < CognitionPhase.values().length; i++) {
            List<CognitionEvent> phaseEvents = recentEvents.stream()
                .filter(e -> e.phase() == CognitionPhase.values()[i])
                .toList();
            long count = phaseEvents.size();
            long successCount = phaseEvents.stream().filter(CognitionEvent::success).count();
            float avgDuration = phaseEvents.isEmpty() ? 0 :
                (float) phaseEvents.stream().mapToDouble(CognitionEvent::durationMs).average().orElse(0);

            phaseStats[i] = new PhaseStats(
                CognitionPhase.values()[i],
                count,
                successCount,
                count > 0 ? (successCount * 100f / count) : 0,
                avgDuration
            );
        }

        // 计算总体指标
        long totalEvents = recentEvents.size();
        long totalSuccess = recentEvents.stream().filter(CognitionEvent::success).count();
        float successRate = totalEvents > 0 ? (totalSuccess * 100f / totalEvents) : 0;
        float avgCycleDuration = recentEvents.isEmpty() ? 0 :
            (float) recentEvents.stream().mapToDouble(CognitionEvent::durationMs).average().orElse(0);

        // 获取最近的认知周期
        List<CognitionCycle> recentCycles = extractCycles(10);

        return new CognitionDashboardData(
            now,
            totalEvents,
            totalSuccess,
            successRate,
            avgCycleDuration,
            phaseStats,
            recentCycles,
            eventHistory.size()
        );
    }

    /**
     * 提取认知周期
     */
    private List<CognitionCycle> extractCycles(int maxCycles) {
        List<CognitionCycle> cycles = new ArrayList<>();
        List<CognitionEvent> currentCycle = new ArrayList<>();

        for (CognitionEvent event : eventHistory) {
            if (event.phase() == CognitionPhase.PERCEPTION && !currentCycle.isEmpty()) {
                // 新的周期开始，保存旧的
                cycles.add(new CognitionCycle(currentCycle));
                currentCycle = new ArrayList<>();
                if (cycles.size() >= maxCycles) break;
            }
            currentCycle.add(event);
        }

        if (!currentCycle.isEmpty() && cycles.size() < maxCycles) {
            cycles.add(new CognitionCycle(currentCycle));
        }

        return cycles;
    }

    /**
     * 认知阶段统计
     */
    public record PhaseStats(
        CognitionPhase phase,
        long eventCount,
        long successCount,
        float successRate,
        float avgDurationMs
    ) {}

    /**
     * 认知周期
     */
    public record CognitionCycle(
        Instant startTime,
        Instant endTime,
        List<CognitionEvent> events,
        float totalDurationMs,
        boolean isComplete
    ) {
        public CognitionCycle(List<CognitionEvent> events) {
            this(
                events.isEmpty() ? Instant.now() : events.get(0).timestamp(),
                events.isEmpty() ? Instant.now() : events.get(events.size() - 1).timestamp(),
                new ArrayList<>(events),
                (float) events.stream().mapToDouble(CognitionEvent::durationMs).sum(),
                events.stream().anyMatch(e -> e.phase() == CognitionPhase.ACTION)
            );
        }
    }

    /**
     * Dashboard数据
     */
    public record CognitionDashboardData(
        Instant timestamp,
        long totalEvents,
        long successCount,
        float successRate,
        float avgCycleDurationMs,
        PhaseStats[] phaseStats,
        List<CognitionCycle> recentCycles,
        int totalHistorySize
    ) {}

    /**
     * 获取阶段摘要
     */
    public String getPhaseSummary() {
        if (eventHistory.isEmpty()) {
            return "No cognition events recorded";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Cognition Phase Summary (Last 1 hour):\n");

        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        List<CognitionEvent> recentEvents = eventHistory.stream()
            .filter(e -> e.timestamp().isAfter(oneHourAgo))
            .toList();

        for (CognitionPhase phase : CognitionPhase.values()) {
            List<CognitionEvent> phaseEvents = recentEvents.stream()
                .filter(e -> e.phase() == phase)
                .toList();
            if (!phaseEvents.isEmpty()) {
                long count = phaseEvents.size();
                long success = phaseEvents.stream().filter(CognitionEvent::success).count();
                sb.append(String.format("  %s: %d events (%.1f%% success)\n",
                    phase, count, count > 0 ? (success * 100f / count) : 0));
            }
        }

        return sb.toString();
    }
}
