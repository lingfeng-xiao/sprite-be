package com.openclaw.digitalbeings.interfaces.rest.being;

import com.openclaw.digitalbeings.application.being.AddIdentityFacetCommand;
import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.being.IdentityFacetView;
import com.openclaw.digitalbeings.application.being.InjectionContextService;
import com.openclaw.digitalbeings.application.being.InjectionContextView;
import com.openclaw.digitalbeings.application.being.ServiceCardView;
import com.openclaw.digitalbeings.application.governance.GovernanceService;
import com.openclaw.digitalbeings.application.governance.GovernanceSummaryView;
import com.openclaw.digitalbeings.application.governance.OwnerProfileCompilationView;
import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.lease.SessionWithLeaseView;
import com.openclaw.digitalbeings.application.lease.StartBeingSessionCommand;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelope;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelopes;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/beings")
public class BeingController {

    private final BeingService beingService;
    private final GovernanceService governanceService;
    private final LeaseService leaseService;
    private final InjectionContextService injectionContextService;

    public BeingController(BeingService beingService, GovernanceService governanceService, LeaseService leaseService, InjectionContextService injectionContextService) {
        this.beingService = beingService;
        this.governanceService = governanceService;
        this.leaseService = leaseService;
        this.injectionContextService = injectionContextService;
    }

    @PostMapping
    public ResponseEntity<RequestEnvelope<BeingView>> createBeing(@RequestBody CreateBeingRequest request) {
        BeingView data = beingService.createBeing(new CreateBeingCommand(request.displayName(), request.actor()));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    /**
     * Registers a runtime session and automatically acquires an authority lease in a single call.
     * This is the primary entry point for OpenClaw host adapters to start a being session.
     */
    @PostMapping("/{beingId}/sessions")
    public ResponseEntity<RequestEnvelope<SessionWithLeaseView>> startBeingSession(
            @PathVariable("beingId") String beingId,
            @RequestBody StartBeingSessionRequest request
    ) {
        SessionWithLeaseView data = leaseService.startBeingSession(
                new StartBeingSessionCommand(beingId, request.hostType(), request.actor())
        );
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/{beingId}")
    public ResponseEntity<RequestEnvelope<BeingView>> getBeing(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(beingService.getBeing(beingId)));
    }

    @GetMapping
    public ResponseEntity<RequestEnvelope<List<BeingView>>> listBeings() {
        return ResponseEntity.ok(RequestEnvelopes.success(beingService.listBeings()));
    }

    @PostMapping("/{beingId}/identity-facets")
    public ResponseEntity<RequestEnvelope<IdentityFacetView>> addIdentityFacet(
            @PathVariable("beingId") String beingId,
            @RequestBody AddIdentityFacetRequest request
    ) {
        IdentityFacetView data = beingService.addIdentityFacet(
                beingId,
                request.kind(),
                request.summary(),
                request.actor(),
                java.time.Clock.systemUTC().instant()
        );
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/{beingId}/governance/summary")
    public ResponseEntity<RequestEnvelope<GovernanceSummaryView>> getGovernanceSummary(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(governanceService.getGovernanceSummary(beingId)));
    }

    @GetMapping("/{beingId}/owner-profile")
    public ResponseEntity<RequestEnvelope<OwnerProfileCompilationView>> getOwnerProfile(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(governanceService.getOwnerProfile(beingId)));
    }

    /**
     * Returns the runtime injection context for a being, used by host adapters to obtain
     * the being's identity, canonical projection, and active session/lease state.
     */
    @GetMapping("/{beingId}/injection-context")
    public ResponseEntity<RequestEnvelope<InjectionContextView>> getInjectionContext(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(injectionContextService.getInjectionContext(beingId)));
    }

    /**
     * Returns the service card (embodiment bundle) for a being.
     * This is a summary view of the being's identity and canonical state, suitable for
     * host adapters to determine the being's current embodiment and readiness.
     */
    @GetMapping("/{beingId}/injection-context/service-card")
    public ResponseEntity<RequestEnvelope<ServiceCardView>> getServiceCard(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(injectionContextService.getServiceCard(beingId)));
    }
}
