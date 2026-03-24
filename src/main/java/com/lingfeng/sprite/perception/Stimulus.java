package com.lingfeng.sprite.perception;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stimulus model for standardized perception input.
 * Represents a single unit of sensory input with metadata for memory processing.
 */
public final class Stimulus {

    private final String id;
    private final String source;
    private final StimulusType type;
    private final Object content;
    private final Instant timestamp;
    private final float salience;
    private final String rawRef;
    private final Map<String, String> contextFragments;

    public Stimulus(
            String id,
            String source,
            StimulusType type,
            Object content,
            Instant timestamp,
            float salience,
            String rawRef,
            Map<String, String> contextFragments
    ) {
        this.id = Objects.requireNonNull(id);
        this.source = Objects.requireNonNull(source);
        this.type = Objects.requireNonNull(type);
        this.content = Objects.requireNonNull(content);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.salience = Math.max(0.0f, Math.min(1.0f, salience));
        this.rawRef = rawRef;
        this.contextFragments = contextFragments != null
            ? Collections.unmodifiableMap(new HashMap<>(contextFragments))
            : Collections.emptyMap();
    }

    public static Stimulus create(
            String source,
            StimulusType type,
            Object content
    ) {
        return new Stimulus(
            UUID.randomUUID().toString(),
            source,
            type,
            content,
            Instant.now(),
            0.5f,
            null,
            Collections.emptyMap()
        );
    }

    public static Stimulus create(
            String source,
            StimulusType type,
            Object content,
            float salience
    ) {
        return new Stimulus(
            UUID.randomUUID().toString(),
            source,
            type,
            content,
            Instant.now(),
            salience,
            null,
            Collections.emptyMap()
        );
    }

    public String id() { return id; }
    public String source() { return source; }
    public StimulusType type() { return type; }
    public Object content() { return content; }
    public Instant timestamp() { return timestamp; }
    public float salience() { return salience; }
    public String rawRef() { return rawRef; }
    public Map<String, String> contextFragments() { return contextFragments; }

    public Stimulus withId(String id) {
        return new Stimulus(id, source, type, content, timestamp, salience, rawRef, contextFragments);
    }

    public Stimulus withSource(String source) {
        return new Stimulus(id, source, type, content, timestamp, salience, rawRef, contextFragments);
    }

    public Stimulus withType(StimulusType type) {
        return new Stimulus(id, source, type, content, timestamp, salience, rawRef, contextFragments);
    }

    public Stimulus withContent(Object content) {
        return new Stimulus(id, source, type, content, timestamp, salience, rawRef, contextFragments);
    }

    public Stimulus withTimestamp(Instant timestamp) {
        return new Stimulus(id, source, type, content, timestamp, salience, rawRef, contextFragments);
    }

    public Stimulus withSalience(float salience) {
        return new Stimulus(id, source, type, content, timestamp, salience, rawRef, contextFragments);
    }

    public Stimulus withRawRef(String rawRef) {
        return new Stimulus(id, source, type, content, timestamp, salience, rawRef, contextFragments);
    }

    public Stimulus withContextFragments(Map<String, String> contextFragments) {
        return new Stimulus(id, source, type, content, timestamp, salience, rawRef, contextFragments);
    }

    public Stimulus addContextFragment(String key, String value) {
        Map<String, String> newFragments = new HashMap<>(contextFragments);
        newFragments.put(key, value);
        return new Stimulus(id, source, type, content, timestamp, salience, rawRef, newFragments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stimulus stimulus)) return false;
        return id.equals(stimulus.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Stimulus{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", type=" + type +
                ", content=" + content +
                ", timestamp=" + timestamp +
                ", salience=" + salience +
                ", rawRef='" + rawRef + '\'' +
                ", contextFragments=" + contextFragments +
                '}';
    }
}
