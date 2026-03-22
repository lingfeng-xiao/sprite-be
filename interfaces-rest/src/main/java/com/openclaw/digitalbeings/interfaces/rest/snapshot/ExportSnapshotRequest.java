package com.openclaw.digitalbeings.interfaces.rest.snapshot;

public record ExportSnapshotRequest(
        String actor
) {

    public ExportSnapshotRequest {
        if (actor == null || actor.isBlank()) {
            actor = "system";
        }
    }
}
