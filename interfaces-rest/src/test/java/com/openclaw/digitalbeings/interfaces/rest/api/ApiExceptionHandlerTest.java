package com.openclaw.digitalbeings.interfaces.rest.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.InjectionContextService;
import com.openclaw.digitalbeings.application.governance.GovernanceService;
import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.snapshot.PortableSnapshotService;
import com.openclaw.digitalbeings.application.snapshot.SnapshotService;
import com.openclaw.digitalbeings.interfaces.rest.being.BeingController;
import com.openclaw.digitalbeings.interfaces.rest.governance.GovernanceController;
import com.openclaw.digitalbeings.interfaces.rest.snapshot.SnapshotController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    @Mock
    private BeingService beingService;

    @Mock
    private GovernanceService governanceService;

    @Mock
    private LeaseService leaseService;

    @Mock
    private InjectionContextService injectionContextService;

    @Mock
    private SnapshotService snapshotService;

    @Mock
    private PortableSnapshotService portableSnapshotService;

    @Test
    void mapsBeingValidationErrorsToIdentityFamily() throws Exception {
        when(beingService.getBeing("missing")).thenThrow(new IllegalArgumentException("Unknown being."));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BeingController(beingService, governanceService, leaseService, injectionContextService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(get("/beings/missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("IDENTITY_VALIDATION"))
                .andExpect(jsonPath("$.error.message").value("Unknown being."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void mapsGraphValidationErrorsToGraphFamily() throws Exception {
        when(governanceService.listOwnerProfileFacts("broken")).thenThrow(new IllegalArgumentException("Malformed governance edge."));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GovernanceController(governanceService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(get("/owner-profile-facts/broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GRAPH_VALIDATION"))
                .andExpect(jsonPath("$.error.message").value("Malformed governance edge."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void mapsSnapshotValidationErrorsToSnapshotFamily() throws Exception {
        when(snapshotService.listSnapshots("broken")).thenThrow(new IllegalArgumentException("Snapshot type is invalid."));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SnapshotController(snapshotService, portableSnapshotService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(get("/snapshots/broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SNAPSHOT_VALIDATION"))
                .andExpect(jsonPath("$.error.message").value("Snapshot type is invalid."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }
}
