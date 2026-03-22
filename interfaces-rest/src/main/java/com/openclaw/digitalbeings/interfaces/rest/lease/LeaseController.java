package com.openclaw.digitalbeings.interfaces.rest.lease;

import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.lease.AcquireAuthorityLeaseCommand;
import com.openclaw.digitalbeings.application.lease.HandoffLeaseCommand;
import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.lease.LeaseView;
import com.openclaw.digitalbeings.application.lease.RegisterRuntimeSessionCommand;
import com.openclaw.digitalbeings.application.lease.ReleaseAuthorityLeaseCommand;
import com.openclaw.digitalbeings.application.lease.RuntimeSessionView;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelope;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelopes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<RequestEnvelope<RuntimeSessionView>> registerSession(@RequestBody RegisterRuntimeSessionRequest request) {
        RuntimeSessionView data = leaseService.registerRuntimeSession(new RegisterRuntimeSessionCommand(
                request.beingId(),
                request.hostType(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @PostMapping("/leases")
    public ResponseEntity<RequestEnvelope<LeaseView>> acquireLease(@RequestBody AcquireAuthorityLeaseRequest request) {
        LeaseView data = leaseService.acquireAuthorityLease(new AcquireAuthorityLeaseCommand(
                request.beingId(),
                request.sessionId(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @PostMapping("/leases/{leaseId}/release")
    public ResponseEntity<RequestEnvelope<LeaseView>> releaseLease(
            @PathVariable("leaseId") String leaseId,
            @RequestBody ReleaseAuthorityLeaseRequest request
    ) {
        LeaseView data = leaseService.releaseAuthorityLease(new ReleaseAuthorityLeaseCommand(
                request.beingId(),
                leaseId,
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    /**
     * Hands off an active authority lease from the current session to a new session.
     * Used for dual-host coordination where the lease must migrate from one host to another atomically.
     */
    @PostMapping("/leases/{leaseId}/handoff")
    public ResponseEntity<RequestEnvelope<LeaseView>> handoffLease(
            @PathVariable("leaseId") String currentLeaseId,
            @RequestBody HandoffLeaseRequest request
    ) {
        LeaseView data = leaseService.handoffLease(new HandoffLeaseCommand(
                request.beingId(),
                currentLeaseId,
                request.newSessionId(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/beings/{beingId}/status")
    public ResponseEntity<RequestEnvelope<BeingView>> getBeingStatus(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(leaseService.getBeing(beingId)));
    }

    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<RequestEnvelope<RuntimeSessionView>> closeSession(
            @PathVariable("sessionId") String sessionId,
            @RequestBody CloseSessionRequest request
    ) {
        RuntimeSessionView data = leaseService.closeSession(request.beingId(), sessionId, request.actor());
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @PostMapping("/leases/{leaseId}/expire")
    public ResponseEntity<RequestEnvelope<LeaseView>> expireLease(
            @PathVariable("leaseId") String leaseId,
            @RequestBody ExpireLeaseRequest request
    ) {
        LeaseView data = leaseService.expireLease(request.beingId(), leaseId, request.actor());
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @PostMapping("/leases/{leaseId}/revoke")
    public ResponseEntity<RequestEnvelope<LeaseView>> revokeLease(
            @PathVariable("leaseId") String leaseId,
            @RequestBody RevokeLeaseRequest request
    ) {
        LeaseView data = leaseService.revokeLease(request.beingId(), leaseId, request.actor());
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/beings/{beingId}/sessions")
    public ResponseEntity<RequestEnvelope<List<RuntimeSessionView>>> listSessions(@PathVariable("beingId") String beingId) {
        List<RuntimeSessionView> data = leaseService.listSessions(beingId);
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/beings/{beingId}/sessions/active")
    public ResponseEntity<RequestEnvelope<List<RuntimeSessionView>>> listActiveSessions(@PathVariable("beingId") String beingId) {
        List<RuntimeSessionView> data = leaseService.listActiveSessions(beingId);
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }
}
