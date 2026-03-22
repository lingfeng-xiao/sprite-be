package com.openclaw.digitalbeings.interfaces.rest.snapshot;

import com.openclaw.digitalbeings.domain.snapshot.PortableSnapshot;

public record ImportSnapshotRequest(
        String beingId,
        PortableSnapshot snapshot,
        String actor
) {

    public ImportSnapshotRequest {
        if (beingId == null || beingId.isBlank()) {
            throw new IllegalArgumentException("beingId must not be blank.");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null.");
        }
        if (actor == null || actor.isBlank()) {
            actor = "system";
        }
    }
}
