package com.openclaw.digitalbeings.application.snapshot;

public record ExportSnapshotCommand(
        String beingId,
        String actor
) {

    public ExportSnapshotCommand {
        beingId = requireText(beingId, "beingId");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
