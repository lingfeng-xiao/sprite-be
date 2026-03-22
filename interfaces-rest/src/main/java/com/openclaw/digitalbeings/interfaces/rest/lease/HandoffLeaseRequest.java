package com.openclaw.digitalbeings.interfaces.rest.lease;

public record HandoffLeaseRequest(
        String beingId,
        String newSessionId,
        String actor
) {
}
