package com.openclaw.digitalbeings.application.being;

import java.util.List;

/**
 * Service card returned to host adapters as the embodiment bundle summary.
 * Derived from the being's canonical projection and identity facets.
 */
public record ServiceCardView(
        String beingId,
        String displayName,
        List<IdentityFacetView> identityFacets,
        String canonicalSummary,
        Long canonicalVersion,
        boolean hasActiveLease
) {}
