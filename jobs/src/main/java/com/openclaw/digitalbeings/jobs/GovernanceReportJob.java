package com.openclaw.digitalbeings.jobs;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Governance job that generates operational reports across all beings.
 * Produces summary statistics for governance, review, and runtime state.
 */
@Component
public class GovernanceReportJob {

    private static final Logger log = LoggerFactory.getLogger(GovernanceReportJob.class);

    private final BeingStore beingStore;

    public GovernanceReportJob(BeingStore beingStore) {
        this.beingStore = beingStore;
    }

    @Scheduled(cron = "0 30 4 * * *") // Run at 4:30am daily
    public GovernanceOperationalReport generateDailyReport() {
        log.info("Generating daily governance operational report...");

        List<BeingSummary> beingSummaries = new ArrayList<>();
        int totalBeings = 0;
        int totalActiveLeases = 0;
        int totalActiveSessions = 0;
        int totalReviewItems = 0;
        int totalPendingReviews = 0;
        int totalAcceptedReviews = 0;

        for (Being being : beingStore.findAll()) {
            totalBeings++;
            totalActiveLeases += being.activeAuthorityLease().isPresent() ? 1 : 0;
            totalActiveSessions += (int) being.runtimeSessions().stream().filter(s -> s.isActive()).count();
            totalReviewItems += being.reviewItems().size();
            totalPendingReviews += (int) being.reviewItems().stream()
                    .filter(r -> !r.isAccepted() && !r.status().name().equals("REJECTED") && !r.status().name().equals("CANCELLED"))
                    .count();
            totalAcceptedReviews += (int) being.reviewItems().stream()
                    .filter(com.openclaw.digitalbeings.domain.review.ReviewItem::isAccepted)
                    .count();

            beingSummaries.add(new BeingSummary(
                    being.beingId().value(),
                    being.displayName(),
                    being.runtimeSessions().size(),
                    (int) being.runtimeSessions().stream().filter(s -> s.isActive()).count(),
                    being.activeAuthorityLease().isPresent(),
                    being.reviewItems().size(),
                    being.ownerProfileFacts().size()
            ));
        }

        GovernanceOperationalReport report = new GovernanceOperationalReport(
                Instant.now(),
                totalBeings,
                totalActiveLeases,
                totalActiveSessions,
                totalReviewItems,
                totalPendingReviews,
                totalAcceptedReviews,
                List.copyOf(beingSummaries)
        );

        log.info("Governance report generated: {} beings, {} active leases, {} active sessions, {} review items ({} pending, {} accepted)",
                totalBeings, totalActiveLeases, totalActiveSessions, totalReviewItems, totalPendingReviews, totalAcceptedReviews);

        return report;
    }

    public record GovernanceOperationalReport(
            Instant generatedAt,
            int totalBeings,
            int totalActiveLeases,
            int totalActiveSessions,
            int totalReviewItems,
            int totalPendingReviews,
            int totalAcceptedReviews,
            List<BeingSummary> beingSummaries
    ) {
        public String render() {
            StringBuilder sb = new StringBuilder();
            sb.append("Governance Operational Report\n");
            sb.append("===========================\n");
            sb.append("Generated: ").append(generatedAt()).append("\n");
            sb.append("Total Beings: ").append(totalBeings()).append("\n");
            sb.append("Active Leases: ").append(totalActiveLeases()).append("\n");
            sb.append("Active Sessions: ").append(totalActiveSessions()).append("\n");
            sb.append("Total Review Items: ").append(totalReviewItems()).append("\n");
            sb.append("Pending Reviews: ").append(totalPendingReviews()).append("\n");
            sb.append("Accepted Reviews: ").append(totalAcceptedReviews()).append("\n");
            if (!beingSummaries().isEmpty()) {
                sb.append("\nBeing Details:\n");
                for (BeingSummary bs : beingSummaries()) {
                    sb.append("  - ").append(bs.displayName())
                            .append(" (id=").append(bs.beingId()).append(")")
                            .append(" sessions=").append(bs.activeSessions())
                            .append(" lease=").append(bs.hasActiveLease() ? "ACTIVE" : "none")
                            .append(" reviews=").append(bs.reviewItemCount())
                            .append("\n");
                }
            }
            return sb.toString();
        }
    }

    public record BeingSummary(
            String beingId,
            String displayName,
            int totalSessions,
            int activeSessions,
            boolean hasActiveLease,
            int reviewItemCount,
            int ownerProfileFactCount
    ) {}
}
