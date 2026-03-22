package com.openclaw.digitalbeings.domain.snapshot;

import com.openclaw.digitalbeings.domain.identity.IdentityFacet;
import com.openclaw.digitalbeings.domain.identity.RelationshipEntity;
import com.openclaw.digitalbeings.domain.review.CanonicalProjection;
import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A self-contained serialization of a Being aggregate.
 * Used for portable snapshot export/import between Neo4j instances.
 *
 * All runtime state (activeSessions, activeLeases) is included as-is so the
 * full continuity context survives the migration. Sessions and leases are
 * re-created with their original IDs so the continuity chain is preserved.
 */
public record PortableSnapshot(
        String version,
        BeingSnapshot being,
        List<IdentityFacetSnapshot> identityFacets,
        List<RelationshipSnapshot> relationships,
        List<SessionSnapshot> sessions,
        List<LeaseSnapshot> leases,
        List<ReviewItemSnapshot> reviewItems,
        List<OwnerProfileSnapshot> ownerProfileFacts,
        List<ManagedAgentSpecSnapshot> managedAgentSpecs,
        CanonicalProjectionSnapshot canonicalProjection,
        List<DomainEventSnapshot> domainEvents,
        Instant exportedAt
) {

    public static final String CURRENT_VERSION = "1.0";

    public record BeingSnapshot(
            String beingId,
            String displayName,
            long continuityEpoch,
            Instant createdAt,
            long revision
    ) {}

    public record IdentityFacetSnapshot(
            String facetId,
            String kind,
            String summary,
            Instant createdAt
    ) {}

    public record RelationshipSnapshot(
            String entityId,
            String kind,
            String displayName,
            Instant createdAt
    ) {}

    public record SessionSnapshot(
            String sessionId,
            String hostType,
            Instant startedAt,
            Instant endedAt
    ) {}

    public record LeaseSnapshot(
            String leaseId,
            String sessionId,
            String status,
            Instant requestedAt,
            Instant grantedAt,
            Instant releasedAt,
            String lastActor
    ) {}

    public record ReviewItemSnapshot(
            String reviewItemId,
            String lane,
            String kind,
            String proposal,
            String status,
            Instant createdAt,
            Instant submittedAt,
            Instant decidedAt,
            String actor
    ) {}

    public record OwnerProfileSnapshot(
            String factId,
            String section,
            String key,
            String summary,
            Instant recordedAt
    ) {}

    public record ManagedAgentSpecSnapshot(
            String specId,
            String role,
            String status,
            Instant registeredAt
    ) {}

    public record CanonicalProjectionSnapshot(
            String projectionId,
            long version,
            Instant generatedAt,
            List<String> acceptedReviewItemIds,
            String contentSummary
    ) {}

    public record DomainEventSnapshot(
            String eventId,
            String eventType,
            String actor,
            Instant occurredAt,
            String summary
    ) {}
}
