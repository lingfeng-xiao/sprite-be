package com.openclaw.digitalbeings.application.being;

import com.openclaw.digitalbeings.application.lease.LeaseView;
import com.openclaw.digitalbeings.application.lease.RuntimeSessionView;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.identity.IdentityFacet;
import com.openclaw.digitalbeings.domain.review.CanonicalProjection;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class InjectionContextService {

    private final BeingStore beingStore;

    public InjectionContextService(BeingStore beingStore) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
    }

    public InjectionContextView getInjectionContext(String beingId) {
        Being being = beingStore.requireById(beingId);

        List<IdentityFacetView> identityFacetViews = being.identityFacets().stream()
                .map(IdentityFacetView::from)
                .toList();

        InjectionContextView.CanonicalProjectionSummary canonicalSummary = being.canonicalProjection()
                .map(this::toCanonicalSummary)
                .orElse(null);

        RuntimeSessionView activeSession = being.activeAuthorityLease()
                .flatMap(lease -> being.runtimeSessions().stream()
                        .filter(session -> session.sessionId().equals(lease.sessionId()))
                        .filter(RuntimeSession::isActive)
                        .findFirst())
                .map(session -> RuntimeSessionView.from(beingId, session))
                .orElse(null);

        LeaseView activeLease = being.activeAuthorityLease()
                .flatMap(lease -> being.runtimeSessions().stream()
                        .filter(session -> session.sessionId().equals(lease.sessionId()))
                        .findFirst()
                        .map(session -> LeaseView.from(beingId, lease, session)))
                .orElse(null);

        return new InjectionContextView(
                beingId,
                being.displayName(),
                identityFacetViews,
                canonicalSummary,
                activeSession,
                activeLease
        );
    }

    private InjectionContextView.CanonicalProjectionSummary toCanonicalSummary(CanonicalProjection projection) {
        return new InjectionContextView.CanonicalProjectionSummary(
                projection.version(),
                projection.acceptedReviewItemIds()
        );
    }

    public ServiceCardView getServiceCard(String beingId) {
        Being being = beingStore.requireById(beingId);

        List<IdentityFacetView> identityFacetViews = being.identityFacets().stream()
                .map(IdentityFacetView::from)
                .toList();

        boolean hasActiveLease = being.activeAuthorityLease().isPresent();

        String canonicalSummary = being.canonicalProjection()
                .map(CanonicalProjection::contentSummary)
                .orElse(null);

        Long canonicalVersion = being.canonicalProjection()
                .map(CanonicalProjection::version)
                .orElse(null);

        return new ServiceCardView(
                beingId,
                being.displayName(),
                identityFacetViews,
                canonicalSummary,
                canonicalVersion,
                hasActiveLease
        );
    }
}
