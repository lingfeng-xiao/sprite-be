package com.openclaw.digitalbeings.application.lease;

public record StartBeingSessionCommand(
        String beingId,
        String hostType,
        String actor
) {

    public StartBeingSessionCommand {
        beingId = requireText(beingId, "beingId");
        hostType = requireText(hostType, "hostType");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
