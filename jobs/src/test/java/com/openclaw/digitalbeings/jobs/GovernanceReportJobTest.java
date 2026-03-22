package com.openclaw.digitalbeings.jobs;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceReportJobTest {

    @Mock
    private BeingStore beingStore;

    private GovernanceReportJob job;

    @BeforeEach
    void setUp() {
        job = new GovernanceReportJob(beingStore);
    }

    @Test
    void generateDailyReport_returnsEmptyReport_whenNoBeings() {
        when(beingStore.findAll()).thenReturn(List.of());

        GovernanceReportJob.GovernanceOperationalReport report = job.generateDailyReport();

        assertThat(report.totalBeings()).isZero();
        assertThat(report.totalActiveLeases()).isZero();
        assertThat(report.totalActiveSessions()).isZero();
        assertThat(report.totalReviewItems()).isZero();
        assertThat(report.beingSummaries()).isEmpty();
    }

    @Test
    void generateDailyReport_countsBeingsCorrectly() {
        Being being1 = Being.create("being-1", "actor", Instant.now());
        being1.registerRuntimeSession("openclaw", "actor", Instant.now());
        being1.acquireAuthorityLease(being1.runtimeSessions().get(0).sessionId(), "actor", Instant.now());

        Being being2 = Being.create("being-2", "actor", Instant.now());
        // being2 has no sessions or leases

        when(beingStore.findAll()).thenReturn(List.of(being1, being2));

        GovernanceReportJob.GovernanceOperationalReport report = job.generateDailyReport();

        assertThat(report.totalBeings()).isEqualTo(2);
        assertThat(report.totalActiveLeases()).isEqualTo(1);
        assertThat(report.totalActiveSessions()).isEqualTo(1);
        assertThat(report.beingSummaries()).hasSize(2);
    }

    @Test
    void generateDailyReport_tracksReviewItems() {
        Being being = Being.create("test-being", "actor", Instant.now());
        being.draftReview("lane1", "feature", "proposal", "actor", Instant.now());
        being.draftReview("lane1", "bugfix", "fix issue", "actor", Instant.now());
        being.submitReview(being.reviewItems().get(0).reviewItemId(), "actor", Instant.now());
        being.reviewItems().get(0).accept("actor", Instant.now());

        when(beingStore.findAll()).thenReturn(List.of(being));

        GovernanceReportJob.GovernanceOperationalReport report = job.generateDailyReport();

        assertThat(report.totalReviewItems()).isEqualTo(2);
        assertThat(report.totalPendingReviews()).isEqualTo(1);
        assertThat(report.totalAcceptedReviews()).isEqualTo(1);
    }

    @Test
    void generateDailyReport_renderProducesReadableOutput() {
        Being being = Being.create("test-being", "actor", Instant.now());
        being.registerRuntimeSession("openclaw", "actor", Instant.now());

        when(beingStore.findAll()).thenReturn(List.of(being));

        GovernanceReportJob.GovernanceOperationalReport report = job.generateDailyReport();
        String rendered = report.render();

        assertThat(rendered).contains("Governance Operational Report");
        assertThat(rendered).contains("Total Beings: 1");
        assertThat(rendered).contains("test-being");
    }
}
