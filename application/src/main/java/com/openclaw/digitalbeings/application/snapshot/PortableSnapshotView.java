package com.openclaw.digitalbeings.application.snapshot;

import com.openclaw.digitalbeings.domain.snapshot.PortableSnapshot;
import java.time.Instant;

/**
 * REST view returned after a portable snapshot export or import.
 */
public record PortableSnapshotView(
        String beingId,
        Instant exportedAt,
        int identityFacetCount,
        int relationshipCount,
        int sessionCount,
        int leaseCount,
        int reviewItemCount,
        int ownerProfileFactCount,
        int managedAgentSpecCount,
        PortableSnapshot snapshot
) {

    public static PortableSnapshotView from(String beingId, PortableSnapshot snapshot, Instant exportedAt) {
        return new PortableSnapshotView(
                beingId,
                exportedAt,
                snapshot.identityFacets().size(),
                snapshot.relationships().size(),
                snapshot.sessions().size(),
                snapshot.leases().size(),
                snapshot.reviewItems().size(),
                snapshot.ownerProfileFacts().size(),
                snapshot.managedAgentSpecs().size(),
                snapshot
        );
    }
}
