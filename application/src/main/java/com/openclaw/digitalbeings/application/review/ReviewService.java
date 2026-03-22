package com.openclaw.digitalbeings.application.review;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.review.CanonicalProjection;
import com.openclaw.digitalbeings.domain.review.ReviewItem;
import java.time.Clock;
import java.util.Objects;

public final class ReviewService {

    private final BeingStore beingStore;
    private final Clock clock;

    public ReviewService(BeingStore beingStore, Clock clock) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ReviewItemView draftReview(DraftReviewCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        ReviewItem reviewItem = being.draftReview(command.lane(), command.kind(), command.proposal(), command.actor(), clock.instant());
        beingStore.save(being);
        return ReviewItemView.from(command.beingId(), reviewItem);
    }

    public ReviewItemView submitReview(SubmitReviewCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        being.submitReview(command.reviewItemId(), command.actor(), clock.instant());
        beingStore.save(being);
        return ReviewItemView.from(command.beingId(), requireReviewItem(being, command.reviewItemId()));
    }

    public ReviewItemView decideReview(DecideReviewCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        ReviewItemStatus targetStatus = switch (command.decision()) {
            case ACCEPTED -> ReviewItemStatus.ACCEPTED;
            case REJECTED -> ReviewItemStatus.REJECTED;
            case DEFERRED -> ReviewItemStatus.DEFERRED;
            default -> throw new IllegalStateException("Unexpected decision: " + command.decision());
        };
        being.decideReview(command.reviewItemId(), targetStatus, command.actor(), clock.instant());
        beingStore.save(being);
        return ReviewItemView.from(command.beingId(), requireReviewItem(being, command.reviewItemId()));
    }

    public CanonicalProjectionView rebuildCanonicalProjection(RebuildCanonicalProjectionCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        CanonicalProjection projection = being.rebuildCanonicalProjection(command.actor(), clock.instant());
        beingStore.save(being);
        return CanonicalProjectionView.from(command.beingId(), projection);
    }

    public CanonicalProjectionView getCanonicalProjection(String beingId) {
        Being being = beingStore.requireById(beingId);
        return being.canonicalProjection()
                .map(projection -> CanonicalProjectionView.from(beingId, projection))
                .orElse(null);
    }

    public void cancelReview(CancelReviewCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        being.cancelReview(command.reviewItemId(), command.actor(), clock.instant());
        beingStore.save(being);
    }

    public ReviewItemView getReviewItem(String beingId, String reviewItemId) {
        Being being = beingStore.requireById(beingId);
        return ReviewItemView.from(beingId, requireReviewItem(being, reviewItemId));
    }

    /**
     * Submits an evolution signal as a review item in the IDENTITY lane.
     * This converts an OpenClaw evolution reflection into a Java-managed review request.
     */
    public ReviewItemView submitEvolutionSignal(SubmitEvolutionSignalCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        ReviewItem reviewItem = being.draftReview(
                "IDENTITY",
                "EVOLUTION_SIGNAL",
                command.proposal(),
                command.actor(),
                clock.instant()
        );
        being.submitReview(reviewItem.reviewItemId(), command.actor(), clock.instant());
        beingStore.save(being);
        return ReviewItemView.from(command.beingId(), requireReviewItem(being, reviewItem.reviewItemId()));
    }

    private static ReviewItem requireReviewItem(Being being, String reviewItemId) {
        return being.reviewItems().stream()
                .filter(candidate -> candidate.reviewItemId().equals(reviewItemId))
                .findFirst()
                .orElseThrow();
    }
}
