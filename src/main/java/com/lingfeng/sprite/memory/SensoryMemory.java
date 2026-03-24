package com.lingfeng.sprite.memory;

import com.lingfeng.sprite.perception.Stimulus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Sensory Memory - 30-second rolling window of raw stimuli.
 * Adapted from MemorySystem.SensoryMemory for database persistence.
 */
public final class SensoryMemory {

    public static final long WINDOW_SECONDS = 30L;

    private final ArrayDeque<Stimulus> recentStimuli;
    private final Instant windowStart;
    private final long maxWindowSeconds;

    public SensoryMemory() {
        this(WINDOW_SECONDS);
    }

    public SensoryMemory(long maxWindowSeconds) {
        this.recentStimuli = new ArrayDeque<>();
        this.windowStart = Instant.now();
        this.maxWindowSeconds = maxWindowSeconds;
    }

    /**
     * Add a stimulus to sensory memory.
     */
    public void add(Stimulus stimulus) {
        Objects.requireNonNull(stimulus, "Stimulus cannot be null");
        recentStimuli.addLast(stimulus);
        pruneExpired();
    }

    /**
     * Remove expired stimuli outside the time window.
     */
    private void pruneExpired() {
        Instant cutoff = Instant.now().minusSeconds(maxWindowSeconds);
        while (!recentStimuli.isEmpty() && recentStimuli.peekFirst().timestamp().isBefore(cutoff)) {
            recentStimuli.removeFirst();
        }
    }

    /**
     * Get all recent stimuli within the time window.
     */
    public List<Stimulus> getRecentStimuli() {
        pruneExpired();
        return new ArrayList<>(recentStimuli);
    }

    /**
     * Get recent stimuli filtered by type.
     */
    public List<Stimulus> getRecentByType(com.lingfeng.sprite.perception.StimulusType type) {
        pruneExpired();
        return recentStimuli.stream()
                .filter(s -> s.type() == type)
                .collect(Collectors.toList());
    }

    /**
     * Detect patterns in recent stimuli for consolidation to working memory.
     */
    public List<DetectedPattern> detectPatterns() {
        pruneExpired();
        List<DetectedPattern> patterns = new ArrayList<>();
        var stimuliByType = recentStimuli.stream()
                .collect(Collectors.groupingBy(Stimulus::type));

        for (var entry : stimuliByType.entrySet()) {
            List<Stimulus> stimuli = entry.getValue();
            if (stimuli.size() >= 3) {
                Instant minTs = stimuli.stream().map(Stimulus::timestamp).min(Instant::compareTo).orElse(Instant.now());
                Instant maxTs = stimuli.stream().map(Stimulus::timestamp).max(Instant::compareTo).orElse(Instant.now());
                patterns.add(new DetectedPattern(
                        entry.getKey(),
                        stimuli.size(),
                        minTs,
                        maxTs,
                        "Detected " + stimuli.size() + " repeated stimuli of type " + entry.getKey()
                ));
            }
        }
        return patterns;
    }

    /**
     * Get the number of stimuli currently in sensory memory.
     */
    public int size() {
        pruneExpired();
        return recentStimuli.size();
    }

    /**
     * Clear all sensory memory.
     */
    public void clear() {
        recentStimuli.clear();
    }

    /**
     * Pattern detected in sensory memory.
     */
    public record DetectedPattern(
            com.lingfeng.sprite.perception.StimulusType type,
            int frequency,
            Instant firstSeen,
            Instant lastSeen,
            String description
    ) {}
}
