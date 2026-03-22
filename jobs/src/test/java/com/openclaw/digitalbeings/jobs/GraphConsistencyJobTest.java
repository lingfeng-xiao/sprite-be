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
class GraphConsistencyJobTest {

    @Mock
    private BeingStore beingStore;

    private GraphConsistencyJob job;

    @BeforeEach
    void setUp() {
        job = new GraphConsistencyJob(beingStore);
    }

    @Test
    void checkGraphConsistency_returnsEmptyReport_whenNoBeings() {
        when(beingStore.findAll()).thenReturn(List.of());

        GraphConsistencyJob.GraphConsistencyReport report = job.checkGraphConsistency();

        assertThat(report.beingsChecked()).isZero();
        assertThat(report.issueCount()).isZero();
        assertThat(report.warningCount()).isZero();
        assertThat(report.hasIssues()).isFalse();
    }

    @Test
    void checkGraphConsistency_findsIssue_whenActiveSessionWithoutLease() {
        // Create a being with an active session but no active lease
        Being being = Being.create("test-being", "test-actor", Instant.now());
        being.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        // No lease acquired - session is active but no lease

        when(beingStore.findAll()).thenReturn(List.of(being));

        GraphConsistencyJob.GraphConsistencyReport report = job.checkGraphConsistency();

        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues()).anySatisfy(issue ->
                assertThat(issue).contains("active sessions but no active lease"));
    }

    @Test
    void checkGraphConsistency_addsWarning_forClosedSessions() {
        // Create a being with a closed session
        Being being = Being.create("test-being", "test-actor", Instant.now());
        being.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        String sessionId = being.runtimeSessions().get(0).sessionId();
        being.closeRuntimeSession(sessionId, "test-actor", Instant.now());

        when(beingStore.findAll()).thenReturn(List.of(being));

        GraphConsistencyJob.GraphConsistencyReport report = job.checkGraphConsistency();

        assertThat(report.warningCount()).isEqualTo(1);
        assertThat(report.warnings()).anySatisfy(warning ->
                assertThat(warning).contains("closed session"));
    }

    @Test
    void checkGraphConsistency_reportsNoIssues_whenHealthyBeing() {
        // Create a healthy being with active session and lease
        Being being = Being.create("test-being", "test-actor", Instant.now());
        being.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        being.acquireAuthorityLease(
                being.runtimeSessions().get(0).sessionId(),
                "test-actor",
                Instant.now()
        );

        when(beingStore.findAll()).thenReturn(List.of(being));

        GraphConsistencyJob.GraphConsistencyReport report = job.checkGraphConsistency();

        assertThat(report.hasIssues()).isFalse();
    }

    @Test
    void checkGraphConsistency_checksMultipleBeings() {
        Being being1 = Being.create("test-being-1", "test-actor", Instant.now());
        being1.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        // No lease for being1

        Being being2 = Being.create("test-being-2", "test-actor", Instant.now());
        being2.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        being2.acquireAuthorityLease(
                being2.runtimeSessions().get(0).sessionId(),
                "test-actor",
                Instant.now()
        );

        when(beingStore.findAll()).thenReturn(List.of(being1, being2));

        GraphConsistencyJob.GraphConsistencyReport report = job.checkGraphConsistency();

        assertThat(report.beingsChecked()).isEqualTo(2);
        assertThat(report.issueCount()).isEqualTo(1); // being1 has issue
    }
}
