package com.openclaw.digitalbeings.jobs;

import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionCleanupJobTest {

    @Mock
    private LeaseService leaseService;

    @Mock
    private BeingStore beingStore;

    private SessionCleanupJob job;

    @BeforeEach
    void setUp() {
        job = new SessionCleanupJob(leaseService, beingStore);
    }

    @Test
    void cleanupOrphanedSessions_doesNothing_whenNoBeings() {
        when(beingStore.findAll()).thenReturn(List.of());

        job.cleanupOrphanedSessions();

        verify(beingStore).findAll();
    }

    @Test
    void cleanupOrphanedSessions_doesNothing_whenAllSessionsActive() {
        Being being = Being.create("test-being", "test-actor", Instant.now());
        being.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        when(beingStore.findAll()).thenReturn(List.of(being));

        job.cleanupOrphanedSessions();

        // Active sessions are not orphaned, so nothing to cleanup
    }

    @Test
    void cleanupOrphanedSessions_closesOrphanedSession() {
        Being being = Being.create("test-being", "test-actor", Instant.now());
        being.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        // Close the session to make it orphaned
        RuntimeSession session = being.runtimeSessions().get(0);
        being.closeRuntimeSession(session.sessionId(), "test-actor", Instant.now());
        when(beingStore.findAll()).thenReturn(List.of(being));

        job.cleanupOrphanedSessions();

        verify(leaseService).closeSession(anyString(), anyString(), eq("SYSTEM"));
    }

    @Test
    void cleanupOrphanedSessions_handlesException_whenCloseSessionFails() {
        Being being = Being.create("test-being", "test-actor", Instant.now());
        being.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        RuntimeSession session = being.runtimeSessions().get(0);
        being.closeRuntimeSession(session.sessionId(), "test-actor", Instant.now());
        when(beingStore.findAll()).thenReturn(List.of(being));
        doThrow(new RuntimeException("Session already closed"))
                .when(leaseService).closeSession(anyString(), anyString(), anyString());

        // Should not throw - job catches exceptions
        job.cleanupOrphanedSessions();

        verify(leaseService).closeSession(anyString(), anyString(), eq("SYSTEM"));
    }

    @Test
    void cleanupOrphanedSessions_processesMultipleBeingsAndSessions() {
        Being being1 = Being.create("test-being-1", "test-actor", Instant.now());
        being1.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        being1.closeRuntimeSession(being1.runtimeSessions().get(0).sessionId(), "test-actor", Instant.now());

        Being being2 = Being.create("test-being-2", "test-actor", Instant.now());
        being2.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        being2.closeRuntimeSession(being2.runtimeSessions().get(0).sessionId(), "test-actor", Instant.now());

        when(beingStore.findAll()).thenReturn(List.of(being1, being2));

        job.cleanupOrphanedSessions();

        verify(leaseService, times(2)).closeSession(anyString(), anyString(), eq("SYSTEM"));
    }
}
