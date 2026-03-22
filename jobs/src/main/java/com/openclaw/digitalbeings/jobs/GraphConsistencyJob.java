package com.openclaw.digitalbeings.jobs;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Governance job that checks graph consistency and reports anomalies.
 * Scans all beings for common integrity issues such as:
 * - Beings with active sessions but no active lease
 * - Beings with orphaned (closed) sessions
 * - Review items stuck in non-terminal states
 */
@Component
public class GraphConsistencyJob {

    private static final Logger log = LoggerFactory.getLogger(GraphConsistencyJob.class);

    private final BeingStore beingStore;

    public GraphConsistencyJob(BeingStore beingStore) {
        this.beingStore = beingStore;
    }

    @Scheduled(cron = "0 0 4 * * *") // Run at 4am daily
    public GraphConsistencyReport checkGraphConsistency() {
        log.info("Running graph consistency check...");
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int beingsChecked = 0;

        for (Being being : beingStore.findAll()) {
            beingsChecked++;
            checkBeingConsistency(being, issues, warnings);
        }

        GraphConsistencyReport report = new GraphConsistencyReport(
                Instant.now(),
                beingsChecked,
                issues.size(),
                warnings.size(),
                List.copyOf(issues),
                List.copyOf(warnings)
        );

        if (report.hasIssues()) {
            log.warn("Graph consistency issues found: {} issues, {} warnings for {} beings checked",
                    report.issueCount(), report.warningCount(), report.beingsChecked());
            for (String issue : report.issues()) {
                log.warn("  - {}", issue);
            }
        } else {
            log.info("Graph consistency check passed: {} beings checked, no issues found", beingsChecked);
        }

        return report;
    }

    private void checkBeingConsistency(Being being, List<String> issues, List<String> warnings) {
        String beingId = being.beingId().value();

        // Check sessions and leases consistency
        boolean hasActiveSession = being.runtimeSessions().stream().anyMatch(RuntimeSession::isActive);
        boolean hasActiveLease = being.activeAuthorityLease().isPresent();

        if (hasActiveSession && !hasActiveLease) {
            issues.add(String.format(
                    "Being %s has active sessions but no active lease - session(s) may be orphaned",
                    beingId));
        }

        // Check for orphaned (closed) sessions
        for (RuntimeSession session : being.runtimeSessions()) {
            if (!session.isActive()) {
                warnings.add(String.format(
                        "Being %s has closed session %s (started at %s)",
                        beingId, session.sessionId(), session.startedAt()));
            }
        }

        // Check review items count - warning if too many pending
        long pendingReviewItems = being.reviewItems().stream()
                .filter(item -> !isTerminalStatus(item.status()))
                .count();
        if (pendingReviewItems > 5) {
            warnings.add(String.format(
                    "Being %s has %d pending review items",
                    beingId, pendingReviewItems));
        }
    }

    private boolean isTerminalStatus(ReviewItemStatus status) {
        return status == ReviewItemStatus.ACCEPTED
                || status == ReviewItemStatus.REJECTED
                || status == ReviewItemStatus.CANCELLED;
    }

    public record GraphConsistencyReport(
            Instant checkedAt,
            int beingsChecked,
            int issueCount,
            int warningCount,
            List<String> issues,
            List<String> warnings
    ) {
        public boolean hasIssues() {
            return issueCount > 0 || warningCount() > 0;
        }
    }
}
