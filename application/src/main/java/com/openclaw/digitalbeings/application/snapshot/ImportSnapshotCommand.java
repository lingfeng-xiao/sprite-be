package com.openclaw.digitalbeings.application.snapshot;

import com.openclaw.digitalbeings.domain.snapshot.PortableSnapshot;

public record ImportSnapshotCommand(
        String beingId,
        PortableSnapshot snapshot,
        String actor
) {

    public ImportSnapshotCommand {
        beingId = requireText(beingId, "beingId");
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null.");
        }
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
