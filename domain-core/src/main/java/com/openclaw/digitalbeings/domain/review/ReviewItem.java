package com.openclaw.digitalbeings.domain.review;

import com.openclaw.digitalbeings.domain.core.DomainRuleViolation;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public final class ReviewItem {

    private final String reviewItemId;
    private final String lane;
    private final String kind;
    private final String proposal;
    private final Instant createdAt;
    private ReviewItemStatus status;
    private Instant updatedAt;
    private String lastActor;

    private ReviewItem(
            String reviewItemId,
            String lane,
            String kind,
            String proposal,
            ReviewItemStatus status,
            Instant createdAt,
            Instant updatedAt,
            String lastActor
    ) {
        this.reviewItemId = requireText(reviewItemId, "reviewItemId");
        this.lane = requireText(lane, "lane");
        this.kind = requireText(kind, "kind");
        this.proposal = requireText(proposal, "proposal");
        if (status == null) {
            throw new IllegalArgumentException("status must not be null.");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("createdAt and updatedAt must not be null.");
        }
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastActor = requireText(lastActor, "lastActor");
    }

    public static ReviewItem draft(String lane, String kind, String proposal, String actor, Instant now) {
        return new ReviewItem(
                UlidFactory.newUlid(),
                lane,
                kind,
                proposal,
                ReviewItemStatus.DRAFT,
                now,
                now,
                actor
        );
    }

    public void submit(String actor, Instant now) {
        if (status != ReviewItemStatus.DRAFT && status != ReviewItemStatus.DEFERRED) {
            throw new DomainRuleViolation("Only draft or deferred review items may be submitted.");
        }
        transitionTo(ReviewItemStatus.SUBMITTED, actor, now);
    }

    public void accept(String actor, Instant now) {
        requireSubmitted("accepted");
        transitionTo(ReviewItemStatus.ACCEPTED, actor, now);
    }

    public void reject(String actor, Instant now) {
        requireSubmitted("rejected");
        transitionTo(ReviewItemStatus.REJECTED, actor, now);
    }

    public void defer(String actor, Instant now) {
        requireSubmitted("deferred");
        transitionTo(ReviewItemStatus.DEFERRED, actor, now);
    }

    public void cancel(String actor, Instant now) {
        if (status == ReviewItemStatus.ACCEPTED || status == ReviewItemStatus.REJECTED || status == ReviewItemStatus.CANCELLED) {
            throw new DomainRuleViolation("Terminal review items cannot be cancelled.");
        }
        transitionTo(ReviewItemStatus.CANCELLED, actor, now);
    }

    public boolean isAccepted() {
        return status == ReviewItemStatus.ACCEPTED;
    }

    public String reviewItemId() {
        return reviewItemId;
    }

    public String lane() {
        return lane;
    }

    public String kind() {
        return kind;
    }

    public String proposal() {
        return proposal;
    }

    public ReviewItemStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String lastActor() {
        return lastActor;
    }

    private void requireSubmitted(String action) {
        if (status != ReviewItemStatus.SUBMITTED) {
            throw new DomainRuleViolation("Only submitted review items may be " + action + ".");
        }
    }

    private void transitionTo(ReviewItemStatus targetStatus, String actor, Instant now) {
        requireText(actor, "actor");
        if (now == null) {
            throw new IllegalArgumentException("now must not be null.");
        }
        if (now.isBefore(updatedAt)) {
            throw new DomainRuleViolation("Review item timestamps must move forward.");
        }
        status = targetStatus;
        updatedAt = now;
        lastActor = actor.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    /**
     * Factory for reconstituting a ReviewItem with a known reviewItemId.
     * Used during portable snapshot import to preserve review item identity.
     */
    public static ReviewItem fromPortableSnapshot(
            String reviewItemId,
            String lane,
            String kind,
            String proposal,
            ReviewItemStatus status,
            Instant createdAt,
            Instant updatedAt,
            String lastActor
    ) {
        return new ReviewItem(reviewItemId, lane, kind, proposal, status, createdAt, updatedAt, lastActor);
    }
}
