package com.openclaw.digitalbeings.domain.runtime;

import com.openclaw.digitalbeings.domain.core.DomainRuleViolation;
import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public final class RuntimeSession {

    private final String sessionId;
    private final String hostType;
    private final Instant startedAt;
    private Instant endedAt;

    private RuntimeSession(String sessionId, String hostType, Instant startedAt, Instant endedAt) {
        this.sessionId = requireText(sessionId, "sessionId");
        this.hostType = requireText(hostType, "hostType");
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null.");
        }
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public static RuntimeSession start(String hostType, Instant startedAt) {
        return new RuntimeSession(UlidFactory.newUlid(), hostType, startedAt, null);
    }

    public void close(Instant closedAt) {
        if (closedAt == null) {
            throw new IllegalArgumentException("closedAt must not be null.");
        }
        if (endedAt != null) {
            throw new DomainRuleViolation("Runtime session is already closed.");
        }
        if (closedAt.isBefore(startedAt)) {
            throw new DomainRuleViolation("Runtime session cannot end before it starts.");
        }
        endedAt = closedAt;
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public boolean isOrphaned(Instant now) {
        return !isActive();
    }

    public String sessionId() {
        return sessionId;
    }

    public String hostType() {
        return hostType;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    /**
     * Factory for reconstituting a RuntimeSession with a known sessionId.
     * Used during portable snapshot import to preserve session identity.
     */
    public static RuntimeSession fromPortableSnapshot(String sessionId, String hostType, Instant startedAt, Instant endedAt) {
        return new RuntimeSession(sessionId, hostType, startedAt, endedAt);
    }
}
