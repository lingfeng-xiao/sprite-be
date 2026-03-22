package com.openclaw.digitalbeings.application.lease;

import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LeaseService {

    private final BeingStore beingStore;
    private final Clock clock;

    public LeaseService(BeingStore beingStore, Clock clock) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public RuntimeSessionView registerRuntimeSession(RegisterRuntimeSessionCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        RuntimeSession runtimeSession = being.registerRuntimeSession(command.hostType(), command.actor(), clock.instant());
        beingStore.save(being);
        return RuntimeSessionView.from(command.beingId(), runtimeSession);
    }

    /**
     * Registers a runtime session and automatically acquires an authority lease for it.
     * This is the primary entry point for OpenClaw host adapters.
     */
    public SessionWithLeaseView startBeingSession(StartBeingSessionCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        RuntimeSession runtimeSession = being.registerRuntimeSession(command.hostType(), command.actor(), clock.instant());
        AuthorityLease lease = being.acquireAuthorityLease(runtimeSession.sessionId(), command.actor(), clock.instant());
        beingStore.save(being);
        return SessionWithLeaseView.from(command.beingId(), runtimeSession, lease);
    }

    public LeaseView acquireAuthorityLease(AcquireAuthorityLeaseCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        AuthorityLease lease = being.acquireAuthorityLease(command.sessionId(), command.actor(), clock.instant());
        beingStore.save(being);
        RuntimeSession session = being.runtimeSessions().stream()
                .filter(candidate -> candidate.sessionId().equals(command.sessionId()))
                .findFirst()
                .orElseThrow();
        return LeaseView.from(command.beingId(), lease, session);
    }

    public LeaseView releaseAuthorityLease(ReleaseAuthorityLeaseCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        being.releaseAuthorityLease(command.leaseId(), command.actor(), clock.instant());
        beingStore.save(being);
        AuthorityLease lease = being.authorityLeases().stream()
                .filter(candidate -> candidate.leaseId().equals(command.leaseId()))
                .findFirst()
                .orElseThrow();
        RuntimeSession session = being.runtimeSessions().stream()
                .filter(candidate -> candidate.sessionId().equals(lease.sessionId()))
                .findFirst()
                .orElseThrow();
        return LeaseView.from(command.beingId(), lease, session);
    }

    /**
     * Hands off the active authority lease from one session to another.
     * Used for dual-host coordination where a new host session takes over the lease atomically.
     */
    public LeaseView handoffLease(HandoffLeaseCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        AuthorityLease newLease = being.handoffAuthorityLease(
                command.currentLeaseId(),
                command.newSessionId(),
                command.actor(),
                clock.instant()
        );
        beingStore.save(being);
        RuntimeSession session = being.runtimeSessions().stream()
                .filter(s -> s.sessionId().equals(newLease.sessionId()))
                .findFirst()
                .orElseThrow();
        return LeaseView.from(command.beingId(), newLease, session);
    }

    public BeingView getBeing(String beingId) {
        return BeingView.from(beingStore.requireById(beingId));
    }

    public RuntimeSessionView closeSession(String beingId, String sessionId, String actor) {
        Being being = beingStore.requireById(beingId);
        // Auto-release any active lease associated with this session before closing the session
        being.authorityLeases().stream()
                .filter(lease -> lease.sessionId().equals(sessionId))
                .filter(AuthorityLease::isActive)
                .findFirst()
                .ifPresent(lease -> being.releaseAuthorityLease(lease.leaseId(), actor, clock.instant()));
        being.closeRuntimeSession(sessionId, actor, clock.instant());
        beingStore.save(being);
        RuntimeSession session = being.requireRuntimeSession(sessionId);
        return RuntimeSessionView.from(beingId, session);
    }

    public LeaseView expireLease(String beingId, String leaseId, String actor) {
        Being being = beingStore.requireById(beingId);
        AuthorityLease lease = being.requireAuthorityLease(leaseId);
        lease.expire(actor, clock.instant());
        beingStore.save(being);
        RuntimeSession session = being.requireRuntimeSession(lease.sessionId());
        return LeaseView.from(beingId, lease, session);
    }

    public LeaseView revokeLease(String beingId, String leaseId, String actor) {
        Being being = beingStore.requireById(beingId);
        AuthorityLease lease = being.requireAuthorityLease(leaseId);
        lease.revoke(actor, clock.instant());
        beingStore.save(being);
        RuntimeSession session = being.requireRuntimeSession(lease.sessionId());
        return LeaseView.from(beingId, lease, session);
    }

    public RuntimeSessionView getSession(String beingId, String sessionId) {
        Being being = beingStore.requireById(beingId);
        RuntimeSession session = being.requireRuntimeSession(sessionId);
        return RuntimeSessionView.from(beingId, session);
    }

    public List<RuntimeSessionView> listSessions(String beingId) {
        Being being = beingStore.requireById(beingId);
        return being.runtimeSessions().stream()
                .map(session -> RuntimeSessionView.from(beingId, session))
                .collect(Collectors.toList());
    }

    public List<RuntimeSessionView> listActiveSessions(String beingId) {
        Being being = beingStore.requireById(beingId);
        return being.runtimeSessions().stream()
                .filter(RuntimeSession::isActive)
                .map(session -> RuntimeSessionView.from(beingId, session))
                .collect(Collectors.toList());
    }
}
