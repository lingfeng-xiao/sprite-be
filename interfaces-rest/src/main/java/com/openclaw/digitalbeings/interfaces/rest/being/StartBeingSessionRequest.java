package com.openclaw.digitalbeings.interfaces.rest.being;

public record StartBeingSessionRequest(
        String beingId,
        String hostType,
        String actor
) {
}
