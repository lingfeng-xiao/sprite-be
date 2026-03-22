package com.openclaw.digitalbeings.domain.runtime;

import com.openclaw.digitalbeings.domain.core.AuthorityLeaseStatus;
import com.openclaw.digitalbeings.domain.core.DomainRuleViolation;
import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public final class AuthorityLease {

    private final String leaseId;
    private final String sessionId;
    private final Instant requestedAt;
    private Instant grantedAt;
    private Instant releasedAt;
    private String lastActor;
    private AuthorityLeaseStatus status;

    private AuthorityLease(
            String leaseId,
            String sessionId,
            AuthorityLeaseStatus status,
            Instant requestedAt,
            Instant grantedAt,
            Instant releasedAt,
            String lastActor
    ) {
        this.leaseId = requireText(leaseId, "leaseId");
        this.sessionId = requireText(sessionId, "sessionId");
        if (status == null) {
            throw new IllegalArgumentException("status must not be null.");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt must not be null.");
        }
        this.status = status;
        this.requestedAt = requestedAt;
        this.grantedAt = grantedAt;
        this.releasedAt = releasedAt;
        this.lastActor = requireText(lastActor, "lastActor");
    }

    public static AuthorityLease activate(String sessionId, String actor, Instant now) {
        return new AuthorityLease(
                UlidFactory.newUlid(),
                sessionId,
                AuthorityLeaseStatus.ACTIVE,
                now,
                now,
                null,
                actor
        );
    }

    public void release(String actor, Instant now) {
        updateTerminalState(AuthorityLeaseStatus.RELEASED, actor, now);
    }

    public void expire(String actor, Instant now) {
        updateTerminalState(AuthorityLeaseStatus.EXPIRED, actor, now);
    }

    public void revoke(String actor, Instant now) {
        updateTerminalState(AuthorityLeaseStatus.REVOKED, actor, now);
    }

    public boolean isActive() {
        return status == AuthorityLeaseStatus.ACTIVE;
    }

    public boolean isExpired() {
        return status == AuthorityLeaseStatus.EXPIRED;
    }

    public String leaseId() {
        return leaseId;
    }

    public String sessionId() {
        return sessionId;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant grantedAt() {
        return grantedAt;
    }

    public Instant releasedAt() {
        return releasedAt;
    }

    public String lastActor() {
        return lastActor;
    }

    public AuthorityLeaseStatus status() {
        return status;
    }

    private void updateTerminalState(AuthorityLeaseStatus target, String actor, Instant now) {
        requireText(actor, "actor");
        if (now == null) {
            throw new IllegalArgumentException("now must not be null.");
        }
        if (!isActive()) {
            throw new DomainRuleViolation("Only an active lease can transition to " + target + ".");
        }
        if (now.isBefore(requestedAt)) {
            throw new DomainRuleViolation("Lease transition time cannot be before the request time.");
        }
        status = target;
        releasedAt = now;
        lastActor = actor.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    /**
     * Factory for reconstituting an AuthorityLease with a known leaseId.
     * Used during portable snapshot import to preserve lease identity.
     */
    public static AuthorityLease fromPortableSnapshot(
            String leaseId,
            String sessionId,
            AuthorityLeaseStatus status,
            Instant requestedAt,
            Instant grantedAt,
            Instant releasedAt,
            String lastActor
    ) {
        return new AuthorityLease(leaseId, sessionId, status, requestedAt, grantedAt, releasedAt, lastActor);
    }
}
