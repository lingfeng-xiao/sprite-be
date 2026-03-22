package com.openclaw.digitalbeings.application.lease;

public record HandoffLeaseCommand(
        String beingId,
        String currentLeaseId,
        String newSessionId,
        String actor
) {

    public HandoffLeaseCommand {
        beingId = requireText(beingId, "beingId");
        currentLeaseId = requireText(currentLeaseId, "currentLeaseId");
        newSessionId = requireText(newSessionId, "newSessionId");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
