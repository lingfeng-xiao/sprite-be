package com.openclaw.digitalbeings.domain.being;

import com.openclaw.digitalbeings.domain.core.BeingId;
import com.openclaw.digitalbeings.domain.core.DomainEventType;
import com.openclaw.digitalbeings.domain.core.DomainRuleViolation;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.core.SnapshotType;
import com.openclaw.digitalbeings.domain.events.DomainEventRecord;
import com.openclaw.digitalbeings.domain.governance.ManagedAgentSpec;
import com.openclaw.digitalbeings.domain.governance.OwnerProfileFact;
import com.openclaw.digitalbeings.domain.identity.IdentityFacet;
import com.openclaw.digitalbeings.domain.identity.RelationshipEntity;
import com.openclaw.digitalbeings.domain.review.CanonicalProjection;
import com.openclaw.digitalbeings.domain.review.ReviewItem;
import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.domain.runtime.HostContract;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import com.openclaw.digitalbeings.domain.snapshot.ContinuitySnapshot;
import com.openclaw.digitalbeings.domain.snapshot.PortableSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Being {

    private final BeingId beingId;
    private final Instant createdAt;
    private final List<IdentityFacet> identityFacets;
    private final List<RelationshipEntity> relationships;
    private final List<HostContract> hostContracts;
    private final List<RuntimeSession> runtimeSessions;
    private final List<AuthorityLease> authorityLeases;
    private final List<ReviewItem> reviewItems;
    private final List<OwnerProfileFact> ownerProfileFacts;
    private final List<ManagedAgentSpec> managedAgentSpecs;
    private final List<ContinuitySnapshot> continuitySnapshots;
    private final List<DomainEventRecord> domainEvents;
    private String displayName;
    private long revision;
    private CanonicalProjection canonicalProjection;

    private Being(BeingId beingId, String displayName, Instant createdAt) {
        this.beingId = beingId;
        this.displayName = requireText(displayName, "displayName");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null.");
        }
        this.createdAt = createdAt;
        this.identityFacets = new ArrayList<>();
        this.relationships = new ArrayList<>();
        this.hostContracts = new ArrayList<>();
        this.runtimeSessions = new ArrayList<>();
        this.authorityLeases = new ArrayList<>();
        this.reviewItems = new ArrayList<>();
        this.ownerProfileFacts = new ArrayList<>();
        this.managedAgentSpecs = new ArrayList<>();
        this.continuitySnapshots = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.revision = 0;
    }

    public static Being reconstitute(
            PortableSnapshot snapshot,
            List<IdentityFacet> identityFacets,
            List<RelationshipEntity> relationships,
            List<RuntimeSession> sessions,
            List<AuthorityLease> leases,
            List<ReviewItem> reviewItems,
            List<OwnerProfileFact> ownerProfileFacts,
            List<ManagedAgentSpec> managedAgentSpecs,
            CanonicalProjection canonicalProjection
    ) {
        Being being = new Being(
                new BeingId(snapshot.being().beingId()),
                snapshot.being().displayName(),
                snapshot.being().createdAt()
        );
        being.revision = snapshot.being().revision();
        // Use reflection-free approach: add items through existing methods
        for (IdentityFacet facet : identityFacets) {
            being.identityFacets.add(facet);
        }
        for (RelationshipEntity rel : relationships) {
            being.relationships.add(rel);
        }
        for (RuntimeSession session : sessions) {
            being.runtimeSessions.add(session);
        }
        for (AuthorityLease lease : leases) {
            being.authorityLeases.add(lease);
        }
        for (ReviewItem item : reviewItems) {
            being.reviewItems.add(item);
        }
        for (OwnerProfileFact fact : ownerProfileFacts) {
            being.ownerProfileFacts.add(fact);
        }
        for (ManagedAgentSpec spec : managedAgentSpecs) {
            being.managedAgentSpecs.add(spec);
        }
        being.canonicalProjection = canonicalProjection;
        return being;
    }

    public static Being create(String displayName, String actor, Instant createdAt) {
        requireText(actor, "actor");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null.");
        }
        Being being = new Being(BeingId.newId(), displayName, createdAt);
        being.recordEvent(DomainEventType.BEING_CREATED, actor, createdAt, "Being created.");
        return being;
    }

    public IdentityFacet addIdentityFacet(String kind, String summary, String actor, Instant now) {
        IdentityFacet facet = IdentityFacet.create(kind, summary, now);
        identityFacets.add(facet);
        recordEvent(DomainEventType.IDENTITY_FACET_ADDED, actor, now, "Identity facet added: " + kind);
        return facet;
    }

    public RelationshipEntity addRelationshipEntity(String kind, String displayName, String actor, Instant now) {
        RelationshipEntity relationshipEntity = RelationshipEntity.create(kind, displayName, now);
        relationships.add(relationshipEntity);
        recordEvent(DomainEventType.RELATIONSHIP_ADDED, actor, now, "Relationship entity added: " + kind);
        return relationshipEntity;
    }

    public HostContract registerHostContract(String hostType, String actor, Instant now) {
        HostContract hostContract = HostContract.active(hostType, now);
        hostContracts.add(hostContract);
        recordEvent(DomainEventType.HOST_CONTRACT_REGISTERED, actor, now, "Host contract registered: " + hostType);
        return hostContract;
    }

    public RuntimeSession registerRuntimeSession(String hostType, String actor, Instant now) {
        RuntimeSession runtimeSession = RuntimeSession.start(hostType, now);
        runtimeSessions.add(runtimeSession);
        recordEvent(DomainEventType.SESSION_REGISTERED, actor, now, "Runtime session registered: " + runtimeSession.sessionId());
        return runtimeSession;
    }

    public AuthorityLease acquireAuthorityLease(String sessionId, String actor, Instant now) {
        RuntimeSession session = runtimeSessions.stream()
                .filter(candidate -> candidate.sessionId().equals(requireText(sessionId, "sessionId")))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("Cannot acquire a lease for an unknown runtime session."));
        if (!session.isActive()) {
            throw new DomainRuleViolation("Cannot acquire a lease for a closed runtime session.");
        }
        if (activeAuthorityLease().isPresent()) {
            throw new DomainRuleViolation("Only one active authoritative lease may exist for a being.");
        }
        AuthorityLease lease = AuthorityLease.activate(session.sessionId(), actor, now);
        authorityLeases.add(lease);
        recordEvent(DomainEventType.LEASE_STATUS_CHANGED, actor, now, "Authority lease activated: " + lease.leaseId());
        return lease;
    }

    public void releaseAuthorityLease(String leaseId, String actor, Instant now) {
        AuthorityLease lease = authorityLeases.stream()
                .filter(candidate -> candidate.leaseId().equals(requireText(leaseId, "leaseId")))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("Cannot release an unknown lease."));
        lease.release(actor, now);
        recordEvent(DomainEventType.LEASE_STATUS_CHANGED, actor, now, "Authority lease released: " + lease.leaseId());
    }

    /**
     * Hands off the active authority lease from the current session to a new session.
     * Used for dual-host coordination where the lease must migrate from one host to another
     * without ever becoming vacant (atomic transition).
     */
    public AuthorityLease handoffAuthorityLease(String currentLeaseId, String newSessionId, String actor, Instant now) {
        String normalizedLeaseId = requireText(currentLeaseId, "currentLeaseId");
        AuthorityLease currentLease = authorityLeases.stream()
                .filter(candidate -> candidate.leaseId().equals(normalizedLeaseId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("Cannot handoff an unknown lease."));

        if (!currentLease.isActive()) {
            throw new DomainRuleViolation("Cannot handoff a lease that is not active.");
        }

        RuntimeSession newSession = runtimeSessions.stream()
                .filter(candidate -> candidate.sessionId().equals(requireText(newSessionId, "newSessionId")))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("Cannot handoff to an unknown runtime session."));

        if (!newSession.isActive()) {
            throw new DomainRuleViolation("Cannot handoff to a closed runtime session.");
        }

        // Release old lease and create new one atomically
        currentLease.release(actor, now);
        AuthorityLease newLease = AuthorityLease.activate(newSession.sessionId(), actor, now);
        authorityLeases.add(newLease);
        recordEvent(DomainEventType.LEASE_STATUS_CHANGED, actor, now,
                "Authority lease handed off from " + currentLeaseId + " to " + newLease.leaseId());
        return newLease;
    }

    public ReviewItem draftReview(String lane, String kind, String proposal, String actor, Instant now) {
        ReviewItem reviewItem = ReviewItem.draft(lane, kind, proposal, actor, now);
        reviewItems.add(reviewItem);
        recordEvent(DomainEventType.REVIEW_ITEM_CHANGED, actor, now, "Review item drafted: " + reviewItem.reviewItemId());
        return reviewItem;
    }

    public void submitReview(String reviewItemId, String actor, Instant now) {
        ReviewItem reviewItem = requireReviewItem(reviewItemId);
        reviewItem.submit(actor, now);
        recordEvent(DomainEventType.REVIEW_ITEM_CHANGED, actor, now, "Review item submitted: " + reviewItem.reviewItemId());
    }

    public void decideReview(String reviewItemId, ReviewItemStatus decision, String actor, Instant now) {
        ReviewItem reviewItem = requireReviewItem(reviewItemId);
        if (decision == null) {
            throw new IllegalArgumentException("decision must not be null.");
        }
        switch (decision) {
            case ACCEPTED -> reviewItem.accept(actor, now);
            case REJECTED -> reviewItem.reject(actor, now);
            case DEFERRED -> reviewItem.defer(actor, now);
            default -> throw new DomainRuleViolation("Unsupported review decision transition: " + decision);
        }
        recordEvent(DomainEventType.REVIEW_ITEM_CHANGED, actor, now, "Review item transitioned to " + decision + ": " + reviewItem.reviewItemId());
    }

    public void cancelReview(String reviewItemId, String actor, Instant now) {
        ReviewItem reviewItem = requireReviewItem(reviewItemId);
        reviewItem.cancel(actor, now);
        recordEvent(DomainEventType.REVIEW_ITEM_CHANGED, actor, now, "Review item cancelled: " + reviewItem.reviewItemId());
    }

    public CanonicalProjection rebuildCanonicalProjection(String actor, Instant now) {
        List<ReviewItem> acceptedReviewItems = reviewItems.stream()
                .filter(ReviewItem::isAccepted)
                .toList();
        canonicalProjection = CanonicalProjection.rebuild(canonicalProjection, acceptedReviewItems, now);
        recordEvent(
                DomainEventType.CANONICAL_PROJECTION_REBUILT,
                actor,
                now,
                "Canonical projection rebuilt at version " + canonicalProjection.version()
        );
        return canonicalProjection;
    }

    public OwnerProfileFact recordOwnerProfileFact(String section, String key, String summary, String actor, Instant now) {
        OwnerProfileFact ownerProfileFact = OwnerProfileFact.create(section, key, summary, now);
        ownerProfileFacts.add(ownerProfileFact);
        recordEvent(DomainEventType.OWNER_PROFILE_FACT_RECORDED, actor, now, "Owner profile fact recorded: " + key);
        return ownerProfileFact;
    }

    public ManagedAgentSpec registerManagedAgentSpec(String role, String status, String actor, Instant now) {
        ManagedAgentSpec managedAgentSpec = ManagedAgentSpec.create(role, status, now);
        managedAgentSpecs.add(managedAgentSpec);
        recordEvent(DomainEventType.MANAGED_AGENT_SPEC_CHANGED, actor, now, "Managed agent spec registered: " + role);
        return managedAgentSpec;
    }

    public ContinuitySnapshot createSnapshot(SnapshotType type, String summary, String actor, Instant now) {
        if (type == SnapshotType.POST_RESTORE && activeAuthorityLease().isPresent()) {
            throw new DomainRuleViolation("A post-restore snapshot cannot be created while an active lease exists.");
        }
        ContinuitySnapshot continuitySnapshot = ContinuitySnapshot.create(type, summary, now);
        continuitySnapshots.add(continuitySnapshot);
        recordEvent(DomainEventType.SNAPSHOT_CREATED, actor, now, "Snapshot created: " + continuitySnapshot.snapshotId());
        return continuitySnapshot;
    }

    public BeingId beingId() {
        return beingId;
    }

    public String displayName() {
        return displayName;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public long revision() {
        return revision;
    }

    public List<IdentityFacet> identityFacets() {
        return List.copyOf(identityFacets);
    }

    public List<RelationshipEntity> relationships() {
        return List.copyOf(relationships);
    }

    public List<HostContract> hostContracts() {
        return List.copyOf(hostContracts);
    }

    public List<RuntimeSession> runtimeSessions() {
        return List.copyOf(runtimeSessions);
    }

    public List<AuthorityLease> authorityLeases() {
        return List.copyOf(authorityLeases);
    }

    public List<ReviewItem> reviewItems() {
        return List.copyOf(reviewItems);
    }

    public Optional<CanonicalProjection> canonicalProjection() {
        return Optional.ofNullable(canonicalProjection);
    }

    public List<OwnerProfileFact> ownerProfileFacts() {
        return List.copyOf(ownerProfileFacts);
    }

    public List<ManagedAgentSpec> managedAgentSpecs() {
        return List.copyOf(managedAgentSpecs);
    }

    public List<ContinuitySnapshot> continuitySnapshots() {
        return List.copyOf(continuitySnapshots);
    }

    public List<DomainEventRecord> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public Optional<AuthorityLease> activeAuthorityLease() {
        return authorityLeases.stream().filter(AuthorityLease::isActive).findFirst();
    }

    public RuntimeSession requireRuntimeSession(String sessionId) {
        String normalizedId = requireText(sessionId, "sessionId");
        return runtimeSessions.stream()
                .filter(candidate -> candidate.sessionId().equals(normalizedId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("Unknown runtime session: " + normalizedId));
    }

    public void closeRuntimeSession(String sessionId, String actor, Instant now) {
        RuntimeSession session = requireRuntimeSession(sessionId);
        session.close(now);
        recordEvent(DomainEventType.LEASE_STATUS_CHANGED, actor, now, "Runtime session closed: " + session.sessionId());
    }

    public AuthorityLease requireAuthorityLease(String leaseId) {
        String normalizedId = requireText(leaseId, "leaseId");
        return authorityLeases.stream()
                .filter(candidate -> candidate.leaseId().equals(normalizedId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("Unknown authority lease: " + normalizedId));
    }

    private ReviewItem requireReviewItem(String reviewItemId) {
        String normalizedId = requireText(reviewItemId, "reviewItemId");
        return reviewItems.stream()
                .filter(candidate -> candidate.reviewItemId().equals(normalizedId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("Unknown review item: " + normalizedId));
    }

    private void recordEvent(DomainEventType eventType, String actor, Instant now, String summary) {
        requireText(actor, "actor");
        if (now == null) {
            throw new IllegalArgumentException("now must not be null.");
        }
        domainEvents.add(DomainEventRecord.create(eventType, actor, now, summary));
        revision += 1;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
