package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.*;
import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.WorldModel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 记忆检索服务 - 为推理引擎提供长期记忆上下文
 *
 * 核心职责：
 * 1. 根据当前情境检索相关的情景记忆（past experiences）
 * 2. 检索相关的语义记忆（knowledge concepts）
 * 3. 检索相关的程序记忆（skill procedures）
 * 4. 检索相关的感知记忆（pattern associations）
 * 5. 将检索结果格式化为推理上下文的一部分
 *
 * ## 检索策略
 *
 * - 基于活动类型（WORK/LEISURE）检索相关记忆
 * - 基于时间上下文（早/中/晚）检索时间模式
 * - 基于情绪状态检索相似情绪下的成功经验
 * - 实现记忆相关性评分和排序
 */
public class MemoryRetrievalService {

    private final MemorySystem.Memory memory;
    private final ZoneId timezone = ZoneId.of("Asia/Shanghai");

    // 记忆检索结果
    public record RetrievalContext(
        List<String> relevantEpisodic,     // 相关情景记忆描述
        List<String> relevantSemantic,     // 相关语义记忆描述
        List<String> relevantProcedural,   // 相关程序记忆描述
        List<String> relevantPatterns,     // 相关感知模式
        float overallRelevance            // 整体相关性评分
    ) {
        public boolean isEmpty() {
            return relevantEpisodic.isEmpty()
                && relevantSemantic.isEmpty()
                && relevantProcedural.isEmpty()
                && relevantPatterns.isEmpty();
        }
    }

    public MemoryRetrievalService(MemorySystem.Memory memory) {
        this.memory = memory;
    }

    /**
     * 根据当前情境检索相关记忆
     *
     * @param context 当前世界模型情境
     * @param mood 当前主人情绪
     * @return 检索到的记忆上下文
     */
    public RetrievalContext retrieve(WorldModel.Context context, OwnerModel.Mood mood) {
        if (memory == null || memory.getLongTerm() == null) {
            return emptyContext();
        }

        LongTermMemory longTerm = memory.getLongTerm();
        List<String> episodic = new ArrayList<>();
        List<String> semantic = new ArrayList<>();
        List<String> procedural = new ArrayList<>();
        List<String> patterns = new ArrayList<>();
        float totalRelevance = 0f;

        // 1. 基于活动类型检索情景记忆
        if (context != null && context.activity() != null) {
            String activityQuery = context.activity().name();
            List<EpisodicEntry> episodicResults = longTerm.recallEpisodic(activityQuery, 5);
            for (EpisodicEntry entry : episodicResults) {
                episodic.add(formatEpisodic(entry));
                totalRelevance += 0.3f;
            }

            // 检索同一活动的过去经历
            List<EpisodicEntry> recentSameActivity = longTerm.getRecentEpisodic(7).stream()
                .filter(e -> {
                    // 检查是否有相同的活动关键词
                    String exp = e.experience().toLowerCase();
                    return exp.contains(activityQuery.toLowerCase()) ||
                           (e.emotion() != null && e.emotion().contains(mood != null ? mood.name() : ""));
                })
                .limit(3)
                .toList();
            for (EpisodicEntry entry : recentSameActivity) {
                String formatted = formatEpisodic(entry);
                if (!episodic.contains(formatted)) {
                    episodic.add(formatted);
                    totalRelevance += 0.2f;
                }
            }
        }

        // 2. 基于情绪检索相似情绪下的记忆
        if (mood != null) {
            String moodStr = mood.name();
            List<EpisodicEntry> moodResults = longTerm.recallEpisodic(moodStr, 3);
            for (EpisodicEntry entry : moodResults) {
                String formatted = formatEpisodic(entry);
                if (!episodic.contains(formatted)) {
                    episodic.add("【类似情绪】" + formatted);
                    totalRelevance += 0.15f;
                }
            }
        }

        // 3. 基于时间上下文检索（早/中/晚）
        String timeContext = getTimeContext();
        if (!timeContext.isEmpty()) {
            List<EpisodicEntry> timeResults = longTerm.recallEpisodic(timeContext, 3);
            for (EpisodicEntry entry : timeResults) {
                String formatted = formatEpisodic(entry);
                if (!episodic.contains(formatted)) {
                    episodic.add("【同时段】" + formatted);
                    totalRelevance += 0.1f;
                }
            }
        }

        // 4. 检索感知模式（perceptive memory）
        if (context != null && context.activity() != null) {
            List<PerceptiveEntry> perceptiveResults = longTerm.recallPerceptive(context.activity().name());
            for (PerceptiveEntry entry : perceptiveResults) {
                patterns.add(formatPerceptive(entry));
                totalRelevance += 0.25f;
            }
        }

        // 5. 检索与当前情境相关的语义记忆
        if (context != null && context.location() != null) {
            List<SemanticEntry> semanticResults = longTerm.recallSemantic(context.location());
            for (SemanticEntry entry : semanticResults) {
                semantic.add(formatSemantic(entry));
                totalRelevance += 0.2f;
            }
        }

        // 限制结果数量，避免上下文过长
        return new RetrievalContext(
            episodic.stream().limit(5).toList(),
            semantic.stream().limit(3).toList(),
            procedural.stream().limit(2).toList(),
            patterns.stream().limit(3).toList(),
            Math.min(totalRelevance, 1.0f)
        );
    }

    /**
     * 检索与动作相关的程序记忆（技能）
     */
    public List<String> retrieveSkillsForAction(String actionType) {
        if (memory == null || memory.getLongTerm() == null) {
            return List.of();
        }

        LongTermMemory longTerm = memory.getLongTerm();
        ProceduralEntry procedural = longTerm.recallProcedural(actionType);

        if (procedural != null) {
            return List.of(formatProcedural(procedural));
        }
        return List.of();
    }

    /**
     * 检索最近的正面经验（用于情绪提振）
     */
    public List<String> retrievePositiveExperiences(int limit) {
        if (memory == null || memory.getLongTerm() == null) {
            return List.of();
        }

        LongTermMemory longTerm = memory.getLongTerm();
        List<EpisodicEntry> recent = longTerm.getRecentEpisodic(30); // 最近30天

        return recent.stream()
            .filter(e -> {
                String emotion = e.emotion() != null ? e.emotion().toLowerCase() : "";
                return emotion.contains("开心") || emotion.contains("满足") ||
                       emotion.contains("excited") || emotion.contains("happy");
            })
            .limit(limit)
            .map(this::formatEpisodic)
            .toList();
    }

    /**
     * 检索最近的负面经验（用于预警）
     */
    public List<String> retrieveNegativeExperiences(int limit) {
        if (memory == null || memory.getLongTerm() == null) {
            return List.of();
        }

        LongTermMemory longTerm = memory.getLongTerm();
        List<EpisodicEntry> recent = longTerm.getRecentEpisodic(30);

        return recent.stream()
            .filter(e -> {
                String emotion = e.emotion() != null ? e.emotion().toLowerCase() : "";
                return emotion.contains("焦虑") || emotion.contains("挫折") ||
                       emotion.contains("沮丧") || emotion.contains("anxious") ||
                       emotion.contains("frustrated");
            })
            .limit(limit)
            .map(this::formatEpisodic)
            .toList();
    }

    /**
     * 获取时间上下文描述
     */
    private String getTimeContext() {
        LocalDateTime now = LocalDateTime.now(timezone);
        int hour = now.getHour();

        if (hour >= 6 && hour < 9) {
            return "早上";
        } else if (hour >= 9 && hour < 12) {
            return "上午";
        } else if (hour >= 12 && hour < 14) {
            return "中午";
        } else if (hour >= 14 && hour < 18) {
            return "下午";
        } else if (hour >= 18 && hour < 22) {
            return "晚上";
        } else {
            return "深夜";
        }
    }

    /**
     * 格式化情景记忆为可读字符串
     */
    private String formatEpisodic(EpisodicEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(formatInstant(entry.timestamp())).append("】");
        sb.append(entry.experience());
        if (entry.lesson() != null && !entry.lesson().isEmpty()) {
            sb.append(" → 教训: ").append(entry.lesson());
        }
        return sb.toString();
    }

    /**
     * 格式化语义记忆为可读字符串
     */
    private String formatSemantic(SemanticEntry entry) {
        return "【知识】" + entry.concept() + ": " + entry.definition();
    }

    /**
     * 格式化程序记忆为可读字符串
     */
    private String formatProcedural(ProceduralEntry entry) {
        return "【技能." + entry.skillName() + "】" +
               "熟练度: " + entry.level() +
               " (执行" + entry.timesPerformed() + "次, 成功率" +
               String.format("%.0f%%", entry.successRate() * 100) + ")";
    }

    /**
     * 格式化感知记忆为可读字符串
     */
    private String formatPerceptive(PerceptiveEntry entry) {
        return "【模式】" + entry.trigger() + " → " + entry.pattern();
    }

    /**
     * 格式化时间戳
     */
    private String formatInstant(Instant instant) {
        if (instant == null) return "未知时间";
        LocalDateTime ldt = instant.atZone(timezone).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        return ldt.format(formatter);
    }

    /**
     * 返回空上下文
     */
    private RetrievalContext emptyContext() {
        return new RetrievalContext(List.of(), List.of(), List.of(), List.of(), 0f);
    }

    /**
     * 构建增强的推理上下文字符串
     */
    public String buildMemoryContextString(RetrievalContext context) {
        if (context.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n## 相关记忆上下文\n");

        if (!context.relevantEpisodic().isEmpty()) {
            sb.append("【情景记忆】\n");
            context.relevantEpisodic().forEach(e -> sb.append("- ").append(e).append("\n"));
        }

        if (!context.relevantSemantic().isEmpty()) {
            sb.append("【语义记忆】\n");
            context.relevantSemantic().forEach(e -> sb.append("- ").append(e).append("\n"));
        }

        if (!context.relevantProcedural().isEmpty()) {
            sb.append("【程序记忆】\n");
            context.relevantProcedural().forEach(e -> sb.append("- ").append(e).append("\n"));
        }

        if (!context.relevantPatterns().isEmpty()) {
            sb.append("【感知模式】\n");
            context.relevantPatterns().forEach(e -> sb.append("- ").append(e).append("\n"));
        }

        sb.append("整体相关性: ").append(String.format("%.0f%%", context.overallRelevance() * 100)).append("\n");

        return sb.toString();
    }
}
