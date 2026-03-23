package com.lingfeng.sprite.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S10-2: 记忆可视化服务
 *
 * 提供记忆系统的可视化数据：
 * - 记忆分布统计
 * - 记忆强度分布
 * - 记忆类型分布
 * - 记忆活跃度分析
 */
public class MemoryVisualizationService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryVisualizationService.class);

    public MemoryVisualizationService() {}

    /**
     * 记忆类型统计
     */
    public record MemoryTypeStats(
        int episodicCount,
        int semanticCount,
        int proceduralCount,
        int perceptiveCount,
        int workingMemoryCount
    ) {}

    /**
     * 记忆强度分布
     */
    public record StrengthDistribution(
        int veryLowCount,    // 0-0.2
        int lowCount,        // 0.2-0.4
        int mediumCount,     // 0.4-0.6
        int highCount,       // 0.6-0.8
        int veryHighCount    // 0.8-1.0
    ) {}

    /**
     * 记忆活跃度
     */
    public record MemoryActivity(
        String memoryId,
        String memoryType,
        Instant lastAccessed,
        int accessCount,
        float strength,
        String preview
    ) {}

    /**
     * 记忆可视化数据
     */
    public record MemoryVisualizationData(
        Instant timestamp,
        MemoryTypeStats typeStats,
        StrengthDistribution strengthDistribution,
        List<MemoryActivity> mostActiveMemories,
        List<MemoryActivity> weakestMemories,
        int totalMemoryCount,
        float averageStrength
    ) {}

    // ==================== 记忆分析接口 ====================

    /**
     * 获取记忆可视化数据（从记忆系统）
     */
    public MemoryVisualizationData generateVisualization(
        List<?> episodicMemories,
        List<?> semanticMemories,
        List<?> proceduralMemories,
        List<?> perceptiveMemories,
        List<?> workingMemories
    ) {
        Instant now = Instant.now();

        // 统计各类型记忆数量
        int episodicCount = episodicMemories != null ? episodicMemories.size() : 0;
        int semanticCount = semanticMemories != null ? semanticMemories.size() : 0;
        int proceduralCount = proceduralMemories != null ? proceduralMemories.size() : 0;
        int perceptiveCount = perceptiveMemories != null ? perceptiveMemories.size() : 0;
        int workingCount = workingMemories != null ? workingMemories.size() : 0;

        MemoryTypeStats typeStats = new MemoryTypeStats(
            episodicCount, semanticCount, proceduralCount, perceptiveCount, workingCount
        );

        // 空的分布（实际需要从记忆系统获取强度数据）
        StrengthDistribution strengthDist = new StrengthDistribution(0, 0, 0, 0, 0);

        // 空的活跃记忆列表（实际需要从记忆系统获取）
        List<MemoryActivity> mostActive = List.of();
        List<MemoryActivity> weakest = List.of();

        int total = episodicCount + semanticCount + proceduralCount + perceptiveCount;

        return new MemoryVisualizationData(
            now,
            typeStats,
            strengthDist,
            mostActive,
            weakest,
            total,
            0.5f  // 默认平均强度
        );
    }

    /**
     * 获取记忆类型描述
     */
    public String getMemoryTypeDescription(MemoryTypeStats stats) {
        int total = stats.episodicCount() + stats.semanticCount() +
                    stats.proceduralCount() + stats.perceptiveCount();

        if (total == 0) {
            return "No memories stored";
        }

        return String.format(
            "Total: %d memories (Episodic: %d, Semantic: %d, Procedural: %d, Perceptive: %d)",
            total,
            stats.episodicCount(),
            stats.semanticCount(),
            stats.proceduralCount(),
            stats.perceptiveCount()
        );
    }

    /**
     * 获取强度分布描述
     */
    public String getStrengthDistributionDescription(StrengthDistribution dist) {
        int total = dist.veryLowCount() + dist.lowCount() + dist.mediumCount() +
                    dist.highCount() + dist.veryHighCount();

        if (total == 0) {
            return "No memory strength data";
        }

        return String.format(
            "Strength Distribution: Very High(%d), High(%d), Medium(%d), Low(%d), Very Low(%d)",
            dist.veryHighCount(), dist.highCount(), dist.mediumCount(),
            dist.lowCount(), dist.veryLowCount()
        );
    }

    /**
     * 记忆时间线数据（用于可视化）
     */
    public record MemoryTimeline(
        Instant startDate,
        Instant endDate,
        List<TimelineEntry> entries
    ) {
        public record TimelineEntry(
            Instant date,
            String memoryType,
            String description,
            float strength
        ) {}
    }

    /**
     * 生成记忆时间线（示例实现）
     */
    public MemoryTimeline generateTimeline(
        List<?> episodicMemories,
        Instant startDate,
        Instant endDate
    ) {
        List<TimelineEntry> entries = new ArrayList<>();

        // 这里需要从实际记忆系统获取数据
        // 示例实现返回空时间线
        return new MemoryTimeline(
            startDate != null ? startDate : Instant.now().minus(30, ChronoUnit.DAYS),
            endDate != null ? endDate : Instant.now(),
            entries
        );
    }
}
