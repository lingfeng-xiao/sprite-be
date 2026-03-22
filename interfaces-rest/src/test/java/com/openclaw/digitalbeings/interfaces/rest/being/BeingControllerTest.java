package com.openclaw.digitalbeings.interfaces.rest.being;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.InjectionContextService;
import com.openclaw.digitalbeings.application.being.InjectionContextView;
import com.openclaw.digitalbeings.application.governance.GovernanceService;
import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.lease.SessionWithLeaseView;
import com.openclaw.digitalbeings.application.lease.LeaseView;
import com.openclaw.digitalbeings.application.lease.RuntimeSessionView;
import com.openclaw.digitalbeings.interfaces.rest.api.ApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BeingControllerTest {

    @Mock
    private BeingService beingService;

    @Mock
    private GovernanceService governanceService;

    @Mock
    private LeaseService leaseService;

    @Mock
    private InjectionContextService injectionContextService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BeingController(beingService, governanceService, leaseService, injectionContextService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createBeingReturnsEnvelope() throws Exception {
        when(beingService.createBeing(any())).thenReturn(sampleBeingView());

        mockMvc.perform(post("/beings")
                        .contentType(APPLICATION_JSON)
                        .content("{\"displayName\":\"guan-guan\",\"actor\":\"codex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.beingId").value("01HZX0000000000000000000000"));
    }

    @Test
    void listBeingsReturnsEnvelope() throws Exception {
        when(beingService.listBeings()).thenReturn(List.of(sampleBeingView()));

        mockMvc.perform(get("/beings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].beingId").value("01HZX0000000000000000000000"));
    }

    @Test
    void startBeingSessionReturnsSessionWithLease() throws Exception {
        when(leaseService.startBeingSession(any())).thenReturn(sampleSessionWithLeaseView());

        mockMvc.perform(post("/beings/01HZX0000000000000000000000/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"beingId\":\"01HZX0000000000000000000000\",\"hostType\":\"openclaw\",\"actor\":\"codex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.session.sessionId").value("sess-001"))
                .andExpect(jsonPath("$.data.lease.leaseId").value("lease-001"));
    }

    private static BeingView sampleBeingView() {
        return new BeingView(
                "01HZX0000000000000000000000",
                "guan-guan",
                1L,
                Instant.parse("2026-03-21T00:00:00Z"),
                0,
                0,
                0,
                0,
                0,
                0,
                null
        );
    }

    private static SessionWithLeaseView sampleSessionWithLeaseView() {
        RuntimeSessionView session = new RuntimeSessionView(
                "01HZX0000000000000000000000",
                "sess-001",
                "openclaw",
                Instant.parse("2026-03-22T00:00:00Z"),
                null
        );
        LeaseView lease = new LeaseView(
                "01HZX0000000000000000000000",
                "lease-001",
                "sess-001",
                "ACTIVE",
                Instant.parse("2026-03-22T00:00:00Z"),
                Instant.parse("2026-03-22T00:00:00Z"),
                null,
                "codex"
        );
        return new SessionWithLeaseView("01HZX0000000000000000000000", session, lease);
    }
}
