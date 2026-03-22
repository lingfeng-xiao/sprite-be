package com.openclaw.digitalbeings.application.review;

public record SubmitEvolutionSignalCommand(
        String beingId,
        String proposal,
        String actor
) {

    public SubmitEvolutionSignalCommand {
        beingId = requireText(beingId, "beingId");
        proposal = requireText(proposal, "proposal");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
