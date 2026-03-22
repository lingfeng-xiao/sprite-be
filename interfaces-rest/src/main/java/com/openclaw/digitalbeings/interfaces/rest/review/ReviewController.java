package com.openclaw.digitalbeings.interfaces.rest.review;

import com.openclaw.digitalbeings.application.review.CancelReviewCommand;
import com.openclaw.digitalbeings.application.review.CanonicalProjectionView;
import com.openclaw.digitalbeings.application.review.DecideReviewCommand;
import com.openclaw.digitalbeings.application.review.DraftReviewCommand;
import com.openclaw.digitalbeings.application.review.RebuildCanonicalProjectionCommand;
import com.openclaw.digitalbeings.application.review.ReviewDecision;
import com.openclaw.digitalbeings.application.review.ReviewItemView;
import com.openclaw.digitalbeings.application.review.ReviewService;
import com.openclaw.digitalbeings.application.review.SubmitEvolutionSignalCommand;
import com.openclaw.digitalbeings.application.review.SubmitReviewCommand;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelope;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelopes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    public ResponseEntity<RequestEnvelope<ReviewItemView>> draftReview(@RequestBody DraftReviewRequest request) {
        ReviewItemView data = reviewService.draftReview(new DraftReviewCommand(
                request.beingId(),
                request.lane(),
                request.kind(),
                request.proposal(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @PostMapping("/reviews/{reviewItemId}/submit")
    public ResponseEntity<RequestEnvelope<ReviewItemView>> submitReview(
            @PathVariable("reviewItemId") String reviewItemId,
            @RequestBody SubmitReviewRequest request
    ) {
        ReviewItemView data = reviewService.submitReview(new SubmitReviewCommand(
                request.beingId(),
                reviewItemId,
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @PostMapping("/reviews/{reviewItemId}/decision")
    public ResponseEntity<RequestEnvelope<ReviewItemView>> decideReview(
            @PathVariable("reviewItemId") String reviewItemId,
            @RequestBody DecideReviewRequest request
    ) {
        ReviewItemView data = reviewService.decideReview(new DecideReviewCommand(
                request.beingId(),
                reviewItemId,
                ReviewDecision.valueOf(request.decision()),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @PostMapping("/reviews/{reviewItemId}/cancel")
    public ResponseEntity<RequestEnvelope<ReviewItemView>> cancelReview(
            @PathVariable("reviewItemId") String reviewItemId,
            @RequestBody CancelReviewRequest request
    ) {
        reviewService.cancelReview(new CancelReviewCommand(
                request.beingId(),
                reviewItemId,
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(reviewService.getReviewItem(request.beingId(), reviewItemId)));
    }

    @PostMapping("/canonical-projections/rebuild")
    public ResponseEntity<RequestEnvelope<CanonicalProjectionView>> rebuildCanonicalProjection(
            @RequestBody RebuildCanonicalProjectionRequest request
    ) {
        CanonicalProjectionView data = reviewService.rebuildCanonicalProjection(new RebuildCanonicalProjectionCommand(
                request.beingId(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/canonical-projections/{beingId}")
    public ResponseEntity<RequestEnvelope<CanonicalProjectionView>> getCanonicalProjection(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(reviewService.getCanonicalProjection(beingId)));
    }

    /**
     * Converts an OpenClaw evolution signal into a Java review item in the IDENTITY lane.
     * The review item is automatically submitted, but IDENTITY_CANDIDATE still requires manual review gate.
     */
    @PostMapping("/beings/{beingId}/evolution-signal")
    public ResponseEntity<RequestEnvelope<ReviewItemView>> submitEvolutionSignal(
            @PathVariable("beingId") String beingId,
            @RequestBody SubmitEvolutionSignalRequest request
    ) {
        ReviewItemView data = reviewService.submitEvolutionSignal(
                new SubmitEvolutionSignalCommand(beingId, request.proposal(), request.actor())
        );
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }
}
