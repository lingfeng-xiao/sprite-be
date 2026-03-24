package com.lingfeng.sprite.memory;

import com.lingfeng.sprite.perception.Stimulus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Long-Term Memory - Persistent storage for episodic, semantic, procedural, and perceptive memories.
 * Adapted from MemorySystem.LongTermMemory for database persistence.
 */
public final class LongTermMemory {

    public static final long RETENTION_DAYS = 365L;

    private final List<EpisodicEntry> episodic;
    private final List<SemanticEntry> semantic;
    private final List<ProceduralEntry> procedural;
    private final List<PerceptiveEntry> perceptive;
    private final Map<String, List<Integer>> episodicIndex;

    public LongTermMemory() {
        this.episodic = new ArrayList<>();
        this.semantic = new ArrayList<>();
        this.procedural = new ArrayList<>();
        this.perceptive = new ArrayList<>();
        this.episodicIndex = new HashMap<>();
    }

    // ==================== Store Operations ====================

    /**
     * Store an episodic memory (event experience).
     */
    public void storeEpisodic(EpisodicEntry entry) {
        Objects.requireNonNull(entry, "EpisodicEntry cannot be null");
        episodic.add(entry);
        String key = entry.timestamp().atZone(java.time.ZoneOffset.UTC).getYear() +
                    "-" + entry.timestamp().atZone(java.time.ZoneOffset.UTC).getMonthValue();
        List<Integer> indices = episodicIndex.getOrDefault(key, new ArrayList<>());
        indices.add(episodic.size() - 1);
        episodicIndex.put(key, indices);
    }

    /**
     * Store a semantic memory (knowledge concept).
     */
    public void storeSemantic(SemanticEntry entry) {
        Objects.requireNonNull(entry, "SemanticEntry cannot be null");
        semantic.removeIf(e -> e.concept().equals(entry.concept()));
        semantic.add(entry);
    }

    /**
     * Store a procedural memory (skill procedure).
     */
    public void storeProcedural(ProceduralEntry entry) {
        Objects.requireNonNull(entry, "ProceduralEntry cannot be null");
        procedural.removeIf(e -> e.skillName().equals(entry.skillName()));
        procedural.add(entry);
    }

    /**
     * Store a perceptive memory (pattern association).
     */
    public void storePerceptive(PerceptiveEntry entry) {
        Objects.requireNonNull(entry, "PerceptiveEntry cannot be null");
        perceptive.removeIf(e -> e.pattern().equals(entry.pattern()) && e.trigger().equals(entry.trigger()));
        perceptive.add(entry);
    }

    // ==================== Recall Operations ====================

    /**
     * Recall episodic memories matching the query.
     */
    public List<EpisodicEntry> recallEpisodic(String query, int limit) {
        Pattern p = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        return episodic.stream()
                .filter(e -> p.matcher(e.experience()).find() ||
                            (e.lesson() != null && p.matcher(e.lesson()).find()) ||
                            (e.emotion() != null && p.matcher(e.emotion()).find()))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<EpisodicEntry> recallEpisodic(String query) {
        return recallEpisodic(query, 10);
    }

    /**
     * Recall semantic memories matching the query.
     */
    public List<SemanticEntry> recallSemantic(String query) {
        Pattern p = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        return semantic.stream()
                .filter(e -> p.matcher(e.concept()).find() ||
                            p.matcher(e.definition()).find())
                .collect(Collectors.toList());
    }

    /**
     * Recall procedural memory by skill name.
     */
    public ProceduralEntry recallProcedural(String skill) {
        return procedural.stream().filter(e -> e.skillName().equals(skill)).findFirst().orElse(null);
    }

    /**
     * Recall perceptive memories matching the pattern.
     */
    public List<PerceptiveEntry> recallPerceptive(String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        return perceptive.stream()
                .filter(e -> p.matcher(e.pattern()).find())
                .collect(Collectors.toList());
    }

    /**
     * Get recent episodic memories within specified days.
     */
    public List<EpisodicEntry> getRecentEpisodic(int days) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        return episodic.stream()
                .filter(e -> e.timestamp().isAfter(cutoff))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .collect(Collectors.toList());
    }

    public List<EpisodicEntry> getRecentEpisodic() {
        return getRecentEpisodic(7);
    }

    /**
     * Update skill level after practice.
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

    // ==================== Statistics ====================

    /**
     * Get memory statistics.
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
     * Prune episodic memories older than retention period.
     */
    public void pruneOldEntries(long retentionDays) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        episodic.removeIf(e -> e.timestamp().isBefore(cutoff));
    }

    public void pruneOldEntries() {
        pruneOldEntries(RETENTION_DAYS);
    }

    // ==================== Getters for Persistence ====================

    public List<EpisodicEntry> getAllEpisodic() {
        return new ArrayList<>(episodic);
    }

    public List<SemanticEntry> getAllSemantic() {
        return new ArrayList<>(semantic);
    }

    public List<ProceduralEntry> getAllProcedural() {
        return new ArrayList<>(procedural);
    }

    public List<PerceptiveEntry> getAllPerceptive() {
        return new ArrayList<>(perceptive);
    }

    // ==================== Entry Records ====================

    /**
     * Episodic memory entry (event experience).
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
     * Semantic memory entry (knowledge concept).
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
     * Procedural memory entry (skill procedure).
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
     * Perceptive memory entry (pattern association).
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
     * Memory statistics.
     */
    public record MemoryStats(
            int episodicCount,
            int semanticCount,
            int proceduralCount,
            int perceptiveCount
    ) {}
}
