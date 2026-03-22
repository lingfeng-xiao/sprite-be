package com.lingfeng.sprite;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 记忆系统 - 三层记忆架构
 *
 * ## 架构设计
 *
 * 模拟人类记忆的层次结构，实现持续学习和知识积累：
 *
 * ```
 * 感官记忆 (30秒滚动窗口)
 *     ↓ 模式检测
 * 工作记忆 (7项上限，米勒定律)
 *     ↓ 遗忘/固化
 * 长期记忆 (持久化)
 *     ├── 情景记忆 (episodic) - 事件经历
 *     ├── 语义记忆 (semantic) - 知识概念
 *     ├── 程序记忆 (procedural) - 技能过程
 *     └── 感知记忆 (perceptive) - 模式关联
 * ```
 *
 * ## 配置常量
 *
 * - [SENSORY_WINDOW_SECONDS] = 30 - 感官记忆窗口
 * - [WORKING_MEMORY_MAX_ITEMS] = 7 - 工作记忆上限（米勒定律）
 * - [LONG_TERM_RETENTION_DAYS] = 365 - 长期记忆保留天数
 */
public final class MemorySystem {

    // 配置常量
    public static final long SENSORY_WINDOW_SECONDS = 30L;
    public static final int WORKING_MEMORY_MAX_ITEMS = 7;
    public static final long LONG_TERM_RETENTION_DAYS = 365L;

    private MemorySystem() {}

    // ==================== 枚举类型 ====================

    public enum StimulusType {
        VISUAL, AUDITORY, TEXT, COMMAND,
        EMOTIONAL, SYSTEM, ENVIRONMENT
    }

    public enum StoreType {
        EPISODIC, SEMANTIC, PROCEDURAL, PERCEPTIVE
    }

    // ==================== 感知刺激 ====================

    /**
     * 感知刺激（原始输入）
     */
    public record Stimulus(
        String id,
        StimulusType type,
        Object content,
        String source,
        Instant timestamp,
        float intensity,
        java.util.Map<String, Object> metadata
    ) {
        public Stimulus {
            Objects.requireNonNull(id);
            Objects.requireNonNull(type);
            Objects.requireNonNull(content);
            Objects.requireNonNull(source);
            Objects.requireNonNull(timestamp);
            metadata = metadata != null ? java.util.Collections.unmodifiableMap(new java.util.HashMap<>(metadata)) : java.util.Collections.emptyMap();
        }

        public Stimulus(String id, StimulusType type, Object content, String source, Instant timestamp) {
            this(id, type, content, source, timestamp, 1.0f, java.util.Collections.emptyMap());
        }

        public Stimulus(String id, StimulusType type, Object content, String source, Instant timestamp, float intensity) {
            this(id, type, content, source, timestamp, intensity, java.util.Collections.emptyMap());
        }
    }

    // ==================== 感官记忆 ====================

    public record Pattern(
        StimulusType type,
        int frequency,
        Instant firstSeen,
        Instant lastSeen,
        String description
    ) {
        public Pattern {
            Objects.requireNonNull(type);
            Objects.requireNonNull(firstSeen);
            Objects.requireNonNull(lastSeen);
            Objects.requireNonNull(description);
        }
    }

    /**
     * 感官记忆（原始感知，30秒滚动窗口）
     */
    public static class SensoryMemory {
        private final ArrayDeque<Stimulus> recentStimuli = new ArrayDeque<>();
        private final Instant windowStart = Instant.now();

        /**
         * 添加刺激
         */
        public void add(Stimulus stimulus) {
            recentStimuli.addLast(stimulus);
            // 清理过期刺激
            Instant cutoff = Instant.now().minusSeconds(SENSORY_WINDOW_SECONDS);
            while (!recentStimuli.isEmpty() && recentStimuli.peekFirst().timestamp().isBefore(cutoff)) {
                recentStimuli.removeFirst();
            }
        }

        /**
         * 获取最近刺激
         */
        public List<Stimulus> getRecentStimuli() {
            return new ArrayList<>(recentStimuli);
        }

        /**
         * 获取某类型的最近刺激
         */
        public List<Stimulus> getRecentByType(StimulusType type) {
            return recentStimuli.stream()
                .filter(s -> s.type() == type)
                .toList();
        }

        /**
         * 检测模式（用于固化为工作记忆）
         */
        public List<Pattern> detectPattern() {
            List<Pattern> patterns = new ArrayList<>();
            var stimuliByType = recentStimuli.stream().collect(java.util.stream.Collectors.groupingBy(Stimulus::type));

            for (var entry : stimuliByType.entrySet()) {
                List<Stimulus> stimuli = entry.getValue();
                if (stimuli.size() >= 3) {
                    Instant minTs = stimuli.stream().map(Stimulus::timestamp).min(Instant::compareTo).orElse(Instant.now());
                    Instant maxTs = stimuli.stream().map(Stimulus::timestamp).max(Instant::compareTo).orElse(Instant.now());
                    patterns.add(new Pattern(
                        entry.getKey(),
                        stimuli.size(),
                        minTs,
                        maxTs,
                        "检测到 " + entry.getKey() + " 类型的重复刺激"
                    ));
                }
            }
            return patterns;
        }

        /**
         * 清理
         */
        public void clear() {
            recentStimuli.clear();
        }
    }

    // ==================== 工作记忆 ====================

    /**
     * 工作记忆项
     */
    public record WorkingMemoryItem(
        String id,
        Object content,
        String abstraction,
        Stimulus source,
        int accessCount,
        Instant lastAccessed,
        float relevance,
        Instant createdAt
    ) {
        public WorkingMemoryItem {
            Objects.requireNonNull(id);
            Objects.requireNonNull(content);
            Objects.requireNonNull(abstraction);
            Objects.requireNonNull(source);
            Objects.requireNonNull(lastAccessed);
            Objects.requireNonNull(createdAt);
        }

        public WorkingMemoryItem(String id, Object content, String abstraction, Stimulus source) {
            this(id, content, abstraction, source, 0, Instant.now(), 0.5f, Instant.now());
        }

        public WorkingMemoryItem(String id, Object content, String abstraction, Stimulus source, float relevance) {
            this(id, content, abstraction, source, 0, Instant.now(), relevance, Instant.now());
        }
    }

    /**
     * 工作记忆（当前会话，7项上限）
     */
    public static class WorkingMemory {
        private final List<WorkingMemoryItem> items = new ArrayList<>();
        private final int maxItems = WORKING_MEMORY_MAX_ITEMS;

        /**
         * 添加项（如果满了，移除最不相关的）
         */
        public void add(WorkingMemoryItem item) {
            items.removeIf(it -> it.id().equals(item.id()));  // 避免重复
            items.add(item);
            pruneIfNeeded();
        }

        /**
         * 添加感官记忆中的模式
         */
        public WorkingMemoryItem consolidate(Pattern pattern, String abstraction, Object content) {
            Stimulus stimulus = new Stimulus(
                "pattern-" + pattern.type() + "-" + Instant.now(),
                pattern.type(),
                content,
                "sensory-memory",
                pattern.lastSeen(),
                1.0f
            );
            WorkingMemoryItem item = new WorkingMemoryItem(
                java.util.UUID.randomUUID().toString(),
                content,
                abstraction,
                stimulus,
                0.7f
            );
            add(item);
            return item;
        }

        private void pruneIfNeeded() {
            // 循环删除直到满足大小限制
            while (items.size() > maxItems) {
                // 按相关性和访问频率排序，移除最低的
                items.sort((a, b) -> {
                    int cmp = Float.compare(a.relevance(), b.relevance());
                    if (cmp != 0) return cmp;
                    return Integer.compare(a.accessCount(), b.accessCount());
                });
                items.remove(0);
            }
        }

        /**
         * 访问项
         */
        public WorkingMemoryItem access(String id) {
            for (int i = 0; i < items.size(); i++) {
                WorkingMemoryItem item = items.get(i);
                if (item.id().equals(id)) {
                    WorkingMemoryItem updated = new WorkingMemoryItem(
                        item.id(),
                        item.content(),
                        item.abstraction(),
                        item.source(),
                        item.accessCount() + 1,
                        Instant.now(),
                        item.relevance(),
                        item.createdAt()
                    );
                    items.set(i, updated);
                    return updated;
                }
            }
            return null;
        }

        /**
         * 更新相关性
         */
        public void updateRelevance(String id, float relevance) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id().equals(id)) {
                    WorkingMemoryItem item = items.get(i);
                    items.set(i, new WorkingMemoryItem(
                        item.id(),
                        item.content(),
                        item.abstraction(),
                        item.source(),
                        item.accessCount(),
                        item.lastAccessed(),
                        relevance,
                        item.createdAt()
                    ));
                    break;
                }
            }
        }

        /**
         * 检索相关项
         */
        public List<WorkingMemoryItem> recall(String query) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(query, java.util.regex.Pattern.CASE_INSENSITIVE);
            return items.stream()
                .filter(it -> p.matcher(it.abstraction()).find() ||
                              p.matcher(it.content().toString()).find())
                .sorted((a, b) -> Float.compare(b.relevance(), a.relevance()))
                .toList();
        }

        /**
         * 获取所有项
         */
        public List<WorkingMemoryItem> getAll() {
            return new ArrayList<>(items);
        }

        /**
         * 获取项数
         */
        public int size() {
            return items.size();
        }

        /**
         * 清理
         */
        public void clear() {
            items.clear();
        }
    }

    // ==================== 长期记忆 ====================

    /**
     * 情景记忆（事件经历）
     */
    public record EpisodicEntry(
        String id,
        Instant timestamp,
        String location,
        List<String> people,
        String experience,
        String emotion,
        String outcome,
        String lesson
    ) {
        public EpisodicEntry {
            Objects.requireNonNull(id);
            Objects.requireNonNull(timestamp);
            Objects.requireNonNull(experience);
            if (location == null) location = null;
            people = people != null ? List.copyOf(people) : List.of();
            if (emotion == null) emotion = null;
            if (outcome == null) outcome = null;
            if (lesson == null) lesson = null;
        }

        public EpisodicEntry(String id, Instant timestamp, String experience) {
            this(id, timestamp, null, List.of(), experience, null, null, null);
        }
    }

    /**
     * 语义记忆（知识概念）
     */
    public record SemanticEntry(
        String id,
        String concept,
        String definition,
        List<String> examples,
        List<String> relatedConcepts,
        float confidence,
        Instant createdAt,
        Instant lastAccessed
    ) {
        public SemanticEntry {
            Objects.requireNonNull(id);
            Objects.requireNonNull(concept);
            Objects.requireNonNull(definition);
            examples = examples != null ? List.copyOf(examples) : List.of();
            relatedConcepts = relatedConcepts != null ? List.copyOf(relatedConcepts) : List.of();
            Objects.requireNonNull(createdAt);
            if (lastAccessed == null) lastAccessed = null;
        }

        public SemanticEntry(String id, String concept, String definition) {
            this(id, concept, definition, List.of(), List.of(), 0.5f, Instant.now(), null);
        }
    }

    /**
     * 程序记忆（技能过程）
     */
    public record ProceduralEntry(
        String id,
        String skillName,
        String procedure,
        String level,
        Instant lastPracticed,
        int timesPerformed,
        float successRate
    ) {
        public ProceduralEntry {
            Objects.requireNonNull(id);
            Objects.requireNonNull(skillName);
            Objects.requireNonNull(procedure);
            if (level == null) level = "BASIC";
            if (lastPracticed == null) lastPracticed = null;
        }

        public ProceduralEntry(String id, String skillName, String procedure) {
            this(id, skillName, procedure, "BASIC", null, 0, 0.5f);
        }
    }

    /**
     * 感知记忆（模式关联）
     */
    public record PerceptiveEntry(
        String id,
        String pattern,
        String association,
        String trigger,
        float strength,
        int timesTriggered
    ) {
        public PerceptiveEntry {
            Objects.requireNonNull(id);
            Objects.requireNonNull(pattern);
            Objects.requireNonNull(association);
            Objects.requireNonNull(trigger);
        }

        public PerceptiveEntry(String id, String pattern, String association, String trigger) {
            this(id, pattern, association, trigger, 0.5f, 0);
        }
    }

    /**
     * 长期记忆
     */
    public static class LongTermMemory {
        private final List<EpisodicEntry> episodic = new ArrayList<>();
        private final List<SemanticEntry> semantic = new ArrayList<>();
        private final List<ProceduralEntry> procedural = new ArrayList<>();
        private final List<PerceptiveEntry> perceptive = new ArrayList<>();
        private final java.util.Map<String, List<Integer>> episodicIndex = new java.util.HashMap<>();

        /**
         * 存储情景记忆
         */
        public void storeEpisodic(EpisodicEntry entry) {
            episodic.add(entry);
            String key = entry.timestamp().atZone(java.time.ZoneOffset.UTC).getYear() +
                        "-" + entry.timestamp().atZone(java.time.ZoneOffset.UTC).getMonthValue();
            List<Integer> indices = episodicIndex.getOrDefault(key, new ArrayList<>());
            indices.add(episodic.size() - 1);
            episodicIndex.put(key, indices);
        }

        /**
         * 存储语义记忆
         */
        public void storeSemantic(SemanticEntry entry) {
            semantic.removeIf(e -> e.concept().equals(entry.concept()));
            semantic.add(entry);
        }

        /**
         * 存储程序记忆
         */
        public void storeProcedural(ProceduralEntry entry) {
            procedural.removeIf(e -> e.skillName().equals(entry.skillName()));
            procedural.add(entry);
        }

        /**
         * 存储感知记忆
         */
        public void storePerceptive(PerceptiveEntry entry) {
            perceptive.removeIf(e -> e.pattern().equals(entry.pattern()) && e.trigger().equals(entry.trigger()));
            perceptive.add(entry);
        }

        /**
         * 检索情景记忆
         */
        public List<EpisodicEntry> recallEpisodic(String query, int limit) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(query, java.util.regex.Pattern.CASE_INSENSITIVE);
            return episodic.stream()
                .filter(e -> p.matcher(e.experience()).find() ||
                            (e.lesson() != null && p.matcher(e.lesson()).find()) ||
                            (e.emotion() != null && p.matcher(e.emotion()).find()))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .limit(limit)
                .toList();
        }

        public List<EpisodicEntry> recallEpisodic(String query) {
            return recallEpisodic(query, 10);
        }

        /**
         * 检索语义记忆
         */
        public List<SemanticEntry> recallSemantic(String query) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(query, java.util.regex.Pattern.CASE_INSENSITIVE);
            return semantic.stream()
                .filter(e -> p.matcher(e.concept()).find() ||
                            p.matcher(e.definition()).find())
                .toList();
        }

        /**
         * 检索程序记忆
         */
        public ProceduralEntry recallProcedural(String skill) {
            return procedural.stream().filter(e -> e.skillName().equals(skill)).findFirst().orElse(null);
        }

        /**
         * 检索感知记忆
         */
        public List<PerceptiveEntry> recallPerceptive(String pattern) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            return perceptive.stream()
                .filter(e -> p.matcher(e.pattern()).find())
                .toList();
        }

        /**
         * 获取最近情景
         */
        public List<EpisodicEntry> getRecentEpisodic(int days) {
            Instant cutoff = Instant.now().minus(Duration.ofDays(days));
            return episodic.stream()
                .filter(e -> e.timestamp().isAfter(cutoff))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .toList();
        }

        public List<EpisodicEntry> getRecentEpisodic() {
            return getRecentEpisodic(7);
        }

        /**
         * 更新技能熟练度
         */
        public void updateSkillLevel(String skillName, String level, boolean success) {
            ProceduralEntry entry = recallProcedural(skillName);
            if (entry != null) {
                float newSuccessRate;
                if (entry.timesPerformed() > 0) {
                    newSuccessRate = (entry.successRate() * entry.timesPerformed() + (success ? 1f : 0f)) / (entry.timesPerformed() + 1);
                } else {
                    newSuccessRate = success ? 1f : 0f;
                }
                ProceduralEntry updated = new ProceduralEntry(
                    entry.id(),
                    entry.skillName(),
                    entry.procedure(),
                    level,
                    Instant.now(),
                    entry.timesPerformed() + 1,
                    newSuccessRate
                );
                int index = procedural.indexOf(entry);
                procedural.set(index, updated);
            }
        }

        /**
         * 获取所有记忆统计
         */
        public MemoryStats getStats() {
            return new MemoryStats(
                episodic.size(),
                semantic.size(),
                procedural.size(),
                perceptive.size()
            );
        }

        /**
         * 清理过期记忆
         */
        public void pruneOldEntries(long retentionDays) {
            Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
            episodic.removeIf(e -> e.timestamp().isBefore(cutoff));
        }

        public void pruneOldEntries() {
            pruneOldEntries(LONG_TERM_RETENTION_DAYS);
        }
    }

    public record MemoryStats(
        int episodicCount,
        int semanticCount,
        int proceduralCount,
        int perceptiveCount
    ) {}

    // ==================== 完整记忆系统 ====================

    /**
     * 完整记忆系统
     */
    public static class Memory {
        private final SensoryMemory sensory;
        private final WorkingMemory working;
        private final LongTermMemory longTerm;

        public Memory() {
            this(new SensoryMemory(), new WorkingMemory(), new LongTermMemory());
        }

        public Memory(SensoryMemory sensory, WorkingMemory working, LongTermMemory longTerm) {
            this.sensory = sensory != null ? sensory : new SensoryMemory();
            this.working = working != null ? working : new WorkingMemory();
            this.longTerm = longTerm != null ? longTerm : new LongTermMemory();
        }

        /**
         * 处理新感知输入
         */
        public void perceive(Stimulus stimulus) {
            sensory.add(stimulus);
        }

        /**
         * 从模式形成工作记忆
         */
        public WorkingMemoryItem consolidateToWorking(Pattern pattern, String abstraction, Object content) {
            return working.consolidate(pattern, abstraction, content);
        }

        /**
         * 从工作记忆存入长期记忆
         */
        public void storeToLongTerm(WorkingMemoryItem item, StoreType type) {
            switch (type) {
                case EPISODIC -> {
                    String emotion = null;
                    if (item.source().metadata() != null && item.source().metadata().containsKey("emotion")) {
                        Object e = item.source().metadata().get("emotion");
                        if (e instanceof String) emotion = (String) e;
                    }
                    longTerm.storeEpisodic(new EpisodicEntry(
                        java.util.UUID.randomUUID().toString(),
                        item.source().timestamp(),
                        null,
                        List.of(),
                        item.abstraction(),
                        emotion,
                        null,
                        null
                    ));
                    break;
                }
                case SEMANTIC -> {
                    longTerm.storeSemantic(new SemanticEntry(
                        java.util.UUID.randomUUID().toString(),
                        item.abstraction(),
                        item.content().toString()
                    ));
                    break;
                }
                case PROCEDURAL -> {
                    longTerm.storeProcedural(new ProceduralEntry(
                        java.util.UUID.randomUUID().toString(),
                        item.abstraction(),
                        item.content().toString()
                    ));
                    break;
                }
                case PERCEPTIVE -> {
                    longTerm.storePerceptive(new PerceptiveEntry(
                        java.util.UUID.randomUUID().toString(),
                        item.source().type().name(),
                        item.abstraction(),
                        item.source().source()
                    ));
                    break;
                }
            }
        }

        /**
         * 检索记忆
         */
        public RecallResult recall(String query) {
            return new RecallResult(
                working.recall(query),
                longTerm.recallEpisodic(query),
                longTerm.recallSemantic(query)
            );
        }

        /**
         * 获取记忆系统状态
         */
        public MemoryStatus getStatus() {
            return new MemoryStatus(
                sensory.getRecentStimuli().size(),
                working.size(),
                longTerm.getStats()
            );
        }

        public SensoryMemory getSensory() { return sensory; }
        public WorkingMemory getWorking() { return working; }
        public LongTermMemory getLongTerm() { return longTerm; }
    }

    public record RecallResult(
        List<WorkingMemoryItem> workingItems,
        List<EpisodicEntry> episodic,
        List<SemanticEntry> semantic
    ) {}

    public record MemoryStatus(
        int sensoryStimuliCount,
        int workingMemoryItems,
        MemoryStats longTermStats
    ) {}
}
