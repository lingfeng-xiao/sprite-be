package com.openclaw.digitalbeings.jobs;

import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseExpiryJobTest {

    @Mock
    private LeaseService leaseService;

    @Mock
    private BeingStore beingStore;

    private LeaseExpiryJob job;

    @BeforeEach
    void setUp() {
        job = new LeaseExpiryJob(leaseService, beingStore);
    }

    @Test
    void scanAndExpireLeases_doesNothing_whenNoBeings() {
        when(beingStore.findAll()).thenReturn(List.of());

        job.scanAndExpireLeases();

        verify(beingStore).findAll();
    }

    @Test
    void scanAndExpireLeases_doesNothing_whenNoExpiredLeases() {
        Being being = createBeingWithActiveLease();
        when(beingStore.findAll()).thenReturn(List.of(being));

        job.scanAndExpireLeases();

        // Active leases are not expired, so nothing to do
    }

    @Test
    void scanAndExpireLeases_callsExpireLease_whenLeaseIsExpired() {
        Being being = createBeingWithExpiredLease();
        when(beingStore.findAll()).thenReturn(List.of(being));

        job.scanAndExpireLeases();

        verify(leaseService).expireLease(anyString(), anyString(), anyString());
    }

    @Test
    void scanAndExpireLeases_handlesException_whenExpireLeaseFails() {
        Being being = createBeingWithExpiredLease();
        when(beingStore.findAll()).thenReturn(List.of(being));
        doThrow(new RuntimeException("Lease already expired"))
                .when(leaseService).expireLease(anyString(), anyString(), anyString());

        // Should not throw - job catches exceptions
        job.scanAndExpireLeases();

        verify(leaseService).expireLease(anyString(), anyString(), anyString());
    }

    @Test
    void scanAndExpireLeases_processesMultipleExpiredLeases() {
        Being being1 = createBeingWithExpiredLease();
        Being being2 = createBeingWithExpiredLease();
        when(beingStore.findAll()).thenReturn(List.of(being1, being2));

        job.scanAndExpireLeases();

        verify(leaseService, times(2)).expireLease(anyString(), anyString(), anyString());
    }

    private Being createBeingWithActiveLease() {
        Being being = Being.create("test-being", "test-actor", Instant.now());
        being.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        being.acquireAuthorityLease(being.runtimeSessions().get(0).sessionId(), "test-actor", Instant.now());
        return being;
    }

    private Being createBeingWithExpiredLease() {
        Being being = Being.create("test-being", "test-actor", Instant.now());
        being.registerRuntimeSession("openclaw", "test-actor", Instant.now());
        AuthorityLease lease = being.acquireAuthorityLease(
            being.runtimeSessions().get(0).sessionId(), "test-actor", Instant.now());
        // Manually expire the lease to simulate time-based expiry
        lease.expire("SYSTEM", Instant.now());
        return being;
    }
}
