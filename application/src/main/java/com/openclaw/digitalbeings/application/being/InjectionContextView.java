package com.openclaw.digitalbeings.application.being;

import com.openclaw.digitalbeings.application.lease.LeaseView;
import com.openclaw.digitalbeings.application.lease.RuntimeSessionView;
import java.util.List;

/**
 * Runtime injection context returned to host adapters (e.g. OpenClaw).
 * Contains the being's identity, canonical projection state, and active runtime session/lease.
 */
public record InjectionContextView(
        String beingId,
        String displayName,
        List<IdentityFacetView> identityFacets,
        CanonicalProjectionSummary canonicalProjection,
        RuntimeSessionView activeSession,
        LeaseView activeLease
) {

    public record CanonicalProjectionSummary(
            Long version,
            List<String> acceptedProposals
    ) {}
}
