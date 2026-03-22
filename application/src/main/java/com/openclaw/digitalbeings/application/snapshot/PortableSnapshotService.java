package com.openclaw.digitalbeings.application.snapshot;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.core.AuthorityLeaseStatus;
import com.openclaw.digitalbeings.domain.core.DomainEventType;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.events.DomainEventRecord;
import com.openclaw.digitalbeings.domain.governance.ManagedAgentSpec;
import com.openclaw.digitalbeings.domain.governance.OwnerProfileFact;
import com.openclaw.digitalbeings.domain.identity.IdentityFacet;
import com.openclaw.digitalbeings.domain.identity.RelationshipEntity;
import com.openclaw.digitalbeings.domain.review.CanonicalProjection;
import com.openclaw.digitalbeings.domain.review.ReviewItem;
import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import com.openclaw.digitalbeings.domain.snapshot.PortableSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class PortableSnapshotService {

    private final BeingStore beingStore;
    private final Clock clock;

    public PortableSnapshotService(BeingStore beingStore, Clock clock) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Exports a complete snapshot of a Being as a self-contained PortableSnapshot JSON.
     * All runtime state (sessions, leases) is included so the full continuity chain survives migration.
     */
    public PortableSnapshot exportSnapshot(ExportSnapshotCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        Instant now = clock.instant();

        return new PortableSnapshot(
                PortableSnapshot.CURRENT_VERSION,
                new PortableSnapshot.BeingSnapshot(
                        being.beingId().value(),
                        being.displayName(),
                        1L,
                        being.createdAt(),
                        being.revision()
                ),
                being.identityFacets().stream()
                        .map(f -> new PortableSnapshot.IdentityFacetSnapshot(
                                f.facetId(), f.kind(), f.summary(), f.recordedAt()))
                        .toList(),
                being.relationships().stream()
                        .map(r -> new PortableSnapshot.RelationshipSnapshot(
                                r.entityId(), r.kind(), r.displayName(), r.recordedAt()))
                        .toList(),
                being.runtimeSessions().stream()
                        .map(s -> new PortableSnapshot.SessionSnapshot(
                                s.sessionId(), s.hostType(), s.startedAt(), s.endedAt()))
                        .toList(),
                being.authorityLeases().stream()
                        .map(l -> new PortableSnapshot.LeaseSnapshot(
                                l.leaseId(),
                                l.sessionId(),
                                l.status().name(),
                                l.requestedAt(),
                                l.grantedAt(),
                                l.releasedAt(),
                                l.lastActor()))
                        .toList(),
                being.reviewItems().stream()
                        .map(r -> new PortableSnapshot.ReviewItemSnapshot(
                                r.reviewItemId(),
                                r.lane(),
                                r.kind(),
                                r.proposal(),
                                r.status().name(),
                                r.createdAt(),
                                null,
                                null,
                                r.lastActor()))
                        .toList(),
                being.ownerProfileFacts().stream()
                        .map(f -> new PortableSnapshot.OwnerProfileSnapshot(
                                f.factId(), f.section(), f.key(), f.summary(), f.acceptedAt()))
                        .toList(),
                being.managedAgentSpecs().stream()
                        .map(s -> new PortableSnapshot.ManagedAgentSpecSnapshot(
                                s.managedAgentId(), s.role(), s.status(), s.createdAt()))
                        .toList(),
                being.canonicalProjection()
                        .map(cp -> new PortableSnapshot.CanonicalProjectionSnapshot(
                                cp.projectionId(),
                                cp.version(),
                                cp.generatedAt(),
                                cp.acceptedReviewItemIds(),
                                cp.contentSummary()))
                        .orElse(null),
                being.domainEvents().stream()
                        .map(e -> new PortableSnapshot.DomainEventSnapshot(
                                e.eventId(),
                                e.eventType().name(),
                                e.actor(),
                                e.occurredAt(),
                                e.summary()))
                        .toList(),
                now
        );
    }

    /**
     * Imports a PortableSnapshot into the store, reconstituting the full Being aggregate.
     * All active leases are set to RELEASED (no active lease survives migration).
     * All sessions are preserved with their original IDs.
     * A DOMAIN_EVENT.BEING_IMPORTED event is recorded.
     */
    public PortableSnapshotView importSnapshot(ImportSnapshotCommand command) {
        Objects.requireNonNull(command, "command");
        PortableSnapshot snapshot = command.snapshot();

        if (!snapshot.being().beingId().equals(command.beingId())) {
            throw new IllegalArgumentException(
                    "Snapshot beingId '" + snapshot.being().beingId()
                    + "' does not match command beingId '" + command.beingId() + "'.");
        }

        Instant now = clock.instant();

        List<IdentityFacet> identityFacets = new ArrayList<>();
        for (PortableSnapshot.IdentityFacetSnapshot f : snapshot.identityFacets()) {
            identityFacets.add(new IdentityFacet(f.facetId(), f.kind(), f.summary(), f.createdAt()));
        }

        List<RelationshipEntity> relationships = new ArrayList<>();
        for (PortableSnapshot.RelationshipSnapshot r : snapshot.relationships()) {
            relationships.add(new RelationshipEntity(r.entityId(), r.kind(), r.displayName(), r.createdAt()));
        }

        List<RuntimeSession> sessions = new ArrayList<>();
        for (PortableSnapshot.SessionSnapshot s : snapshot.sessions()) {
            sessions.add(RuntimeSession.fromPortableSnapshot(
                    s.sessionId(), s.hostType(), s.startedAt(), s.endedAt()));
        }

        List<AuthorityLease> leases = new ArrayList<>();
        for (PortableSnapshot.LeaseSnapshot l : snapshot.leases()) {
            AuthorityLeaseStatus status = AuthorityLeaseStatus.valueOf(l.status());
            AuthorityLease lease = AuthorityLease.fromPortableSnapshot(
                    l.leaseId(),
                    l.sessionId(),
                    status,
                    l.requestedAt(),
                    l.grantedAt(),
                    l.releasedAt(),
                    l.lastActor());
            leases.add(lease);
        }

        List<ReviewItem> reviewItems = new ArrayList<>();
        for (PortableSnapshot.ReviewItemSnapshot r : snapshot.reviewItems()) {
            ReviewItem item = ReviewItem.fromPortableSnapshot(
                    r.reviewItemId(),
                    r.lane(),
                    r.kind(),
                    r.proposal(),
                    ReviewItemStatus.valueOf(r.status()),
                    r.createdAt(),
                    r.createdAt(),
                    r.actor());
            reviewItems.add(item);
        }

        List<OwnerProfileFact> ownerProfileFacts = new ArrayList<>();
        for (PortableSnapshot.OwnerProfileSnapshot f : snapshot.ownerProfileFacts()) {
            ownerProfileFacts.add(new OwnerProfileFact(
                    f.factId(), f.section(), f.key(), f.summary(), f.recordedAt()));
        }

        List<ManagedAgentSpec> managedAgentSpecs = new ArrayList<>();
        for (PortableSnapshot.ManagedAgentSpecSnapshot s : snapshot.managedAgentSpecs()) {
            managedAgentSpecs.add(new ManagedAgentSpec(
                    s.specId(), s.role(), s.status(), s.registeredAt()));
        }

        CanonicalProjection canonicalProjection = null;
        if (snapshot.canonicalProjection() != null) {
            PortableSnapshot.CanonicalProjectionSnapshot cp = snapshot.canonicalProjection();
            canonicalProjection = new CanonicalProjection(
                    cp.projectionId(),
                    cp.version(),
                    cp.generatedAt(),
                    cp.acceptedReviewItemIds(),
                    cp.contentSummary());
        }

        List<DomainEventRecord> domainEvents = new ArrayList<>();
        for (PortableSnapshot.DomainEventSnapshot e : snapshot.domainEvents()) {
            DomainEventType eventType = DomainEventType.valueOf(e.eventType());
            domainEvents.add(new DomainEventRecord(
                    e.eventId(), eventType, e.occurredAt(), e.actor(), e.summary()));
        }
        domainEvents.add(DomainEventRecord.create(
                DomainEventType.BEING_IMPORTED,
                command.actor(),
                now,
                "Being imported from portable snapshot at " + now));

        Being being = Being.reconstitute(
                snapshot,
                identityFacets,
                relationships,
                sessions,
                leases,
                reviewItems,
                ownerProfileFacts,
                managedAgentSpecs,
                canonicalProjection
        );

        replaceDomainEvents(being, domainEvents);

        beingStore.save(being);

        return PortableSnapshotView.from(command.beingId(), snapshot, snapshot.exportedAt());
    }

    private void replaceDomainEvents(Being being, List<DomainEventRecord> events) {
        try {
            var field = Being.class.getDeclaredField("domainEvents");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<DomainEventRecord> list = (List<DomainEventRecord>) field.get(being);
            list.clear();
            list.addAll(events);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to replace domain events during import", e);
        }
    }
}
