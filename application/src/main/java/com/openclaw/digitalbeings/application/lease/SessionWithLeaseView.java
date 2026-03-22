package com.openclaw.digitalbeings.application.lease;

import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;

/**
 * Combined view of a runtime session and its associated authority lease,
 * returned when a session is started with automatic lease acquisition.
 */
public record SessionWithLeaseView(
        String beingId,
        RuntimeSessionView session,
        LeaseView lease
) {

    public static SessionWithLeaseView from(String beingId, RuntimeSession runtimeSession, AuthorityLease lease) {
        RuntimeSessionView sessionView = RuntimeSessionView.from(beingId, runtimeSession);
        LeaseView leaseView = LeaseView.from(beingId, lease, runtimeSession);
        return new SessionWithLeaseView(beingId, sessionView, leaseView);
    }
}
