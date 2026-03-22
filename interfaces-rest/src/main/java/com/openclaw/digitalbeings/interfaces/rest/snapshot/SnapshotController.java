package com.openclaw.digitalbeings.interfaces.rest.snapshot;

import com.openclaw.digitalbeings.application.snapshot.CreateSnapshotCommand;
import com.openclaw.digitalbeings.application.snapshot.ExportSnapshotCommand;
import com.openclaw.digitalbeings.application.snapshot.ImportSnapshotCommand;
import com.openclaw.digitalbeings.application.snapshot.PortableSnapshotService;
import com.openclaw.digitalbeings.application.snapshot.PortableSnapshotView;
import com.openclaw.digitalbeings.application.snapshot.SnapshotService;
import com.openclaw.digitalbeings.application.snapshot.SnapshotView;
import com.openclaw.digitalbeings.domain.core.SnapshotType;
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
@RequestMapping("/snapshots")
public class SnapshotController {

    private final SnapshotService snapshotService;
    private final PortableSnapshotService portableSnapshotService;

    public SnapshotController(SnapshotService snapshotService, PortableSnapshotService portableSnapshotService) {
        this.snapshotService = snapshotService;
        this.portableSnapshotService = portableSnapshotService;
    }

    @PostMapping
    public ResponseEntity<RequestEnvelope<SnapshotView>> createSnapshot(@RequestBody CreateSnapshotRequest request) {
        SnapshotView data = snapshotService.createSnapshot(new CreateSnapshotCommand(
                request.beingId(),
                SnapshotType.valueOf(request.type()),
                request.summary(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/{beingId}")
    public ResponseEntity<RequestEnvelope<List<SnapshotView>>> listSnapshots(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(snapshotService.listSnapshots(beingId)));
    }

    @PostMapping("/beings/{beingId}/export")
    public ResponseEntity<RequestEnvelope<PortableSnapshotView>> exportSnapshot(
            @PathVariable("beingId") String beingId,
            @RequestBody ExportSnapshotRequest request
    ) {
        var snapshot = portableSnapshotService.exportSnapshot(new ExportSnapshotCommand(beingId, request.actor()));
        return ResponseEntity.ok(RequestEnvelopes.success(
                PortableSnapshotView.from(beingId, snapshot, snapshot.exportedAt())));
    }

    @PostMapping("/beings/{beingId}/import")
    public ResponseEntity<RequestEnvelope<PortableSnapshotView>> importSnapshot(
            @PathVariable("beingId") String beingId,
            @RequestBody ImportSnapshotRequest request
    ) {
        var view = portableSnapshotService.importSnapshot(new ImportSnapshotCommand(beingId, request.snapshot(), request.actor()));
        return ResponseEntity.ok(RequestEnvelopes.success(view));
    }
}
