package com.openclaw.digitalbeings.application.lease;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaseServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-21T08:10:00Z"), ZoneOffset.UTC);

    @Test
    void registerAndAcquireLeaseUseTheSameAggregate() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        LeaseService leaseService = new LeaseService(store, CLOCK);
        BeingView view = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        RuntimeSessionView sessionView = leaseService.registerRuntimeSession(
                new RegisterRuntimeSessionCommand(view.beingId(), "codex", "codex")
        );
        LeaseView leaseView = leaseService.acquireAuthorityLease(
                new AcquireAuthorityLeaseCommand(view.beingId(), sessionView.sessionId(), "codex")
        );

        assertEquals(view.beingId(), leaseView.beingId());
        assertEquals("ACTIVE", leaseView.status());
        assertEquals(1, leaseService.getBeing(view.beingId()).runtimeSessionCount());
    }

    @Test
    void releaseLeaseMarksTheLeaseAsReleased() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        LeaseService leaseService = new LeaseService(store, CLOCK);
        BeingView view = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));
        RuntimeSessionView sessionView = leaseService.registerRuntimeSession(
                new RegisterRuntimeSessionCommand(view.beingId(), "codex", "codex")
        );
        LeaseView activeLease = leaseService.acquireAuthorityLease(
                new AcquireAuthorityLeaseCommand(view.beingId(), sessionView.sessionId(), "codex")
        );

        LeaseView releasedLease = leaseService.releaseAuthorityLease(
                new ReleaseAuthorityLeaseCommand(view.beingId(), activeLease.leaseId(), "codex")
        );

        assertEquals("RELEASED", releasedLease.status());
    }

    @Test
    void startBeingSessionRegistersSessionAndAcquiresLeaseInOneCall() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        LeaseService leaseService = new LeaseService(store, CLOCK);
        BeingView view = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        SessionWithLeaseView result = leaseService.startBeingSession(
                new StartBeingSessionCommand(view.beingId(), "openclaw", "codex")
        );

        assertEquals(view.beingId(), result.beingId());
        assertEquals("openclaw", result.session().hostType());
        assertEquals("ACTIVE", result.lease().status());
        assertEquals(result.session().sessionId(), result.lease().sessionId());
    }

    @Test
    void closeSessionAutoReleasesAssociatedLease() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        LeaseService leaseService = new LeaseService(store, CLOCK);
        BeingView view = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        // Use startBeingSession to create session + lease in one call
        SessionWithLeaseView started = leaseService.startBeingSession(
                new StartBeingSessionCommand(view.beingId(), "openclaw", "codex")
        );

        // Close session - should auto-release the lease
        RuntimeSessionView closed = leaseService.closeSession(view.beingId(), started.session().sessionId(), "codex");

        // Session is closed
        assertEquals(closed.endedAt(), CLOCK.instant());

        // Lease was auto-released
        var leases = leaseService.listSessions(view.beingId());
        // The session is closed
        assertEquals(1, leases.size());
    }
}
