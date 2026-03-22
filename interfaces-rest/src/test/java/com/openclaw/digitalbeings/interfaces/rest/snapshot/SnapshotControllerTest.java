package com.openclaw.digitalbeings.interfaces.rest.snapshot;

import com.openclaw.digitalbeings.application.snapshot.PortableSnapshotService;
import com.openclaw.digitalbeings.application.snapshot.SnapshotService;
import com.openclaw.digitalbeings.application.snapshot.SnapshotView;
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
class SnapshotControllerTest {

    @Mock
    private SnapshotService snapshotService;

    @Mock
    private PortableSnapshotService portableSnapshotService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SnapshotController(snapshotService, portableSnapshotService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createSnapshotReturnsEnvelope() throws Exception {
        when(snapshotService.createSnapshot(any())).thenReturn(sampleSnapshotView());

        mockMvc.perform(post("/snapshots")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"beingId":"01HZX0000000000000000000000","type":"MILESTONE","summary":"checkpoint","actor":"codex"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshotId").value("01HZX0000000000000000000001"));
    }

    @Test
    void listSnapshotsReturnsEnvelope() throws Exception {
        when(snapshotService.listSnapshots("01HZX0000000000000000000000"))
                .thenReturn(List.of(sampleSnapshotView()));

        mockMvc.perform(get("/snapshots/01HZX0000000000000000000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].snapshotId").value("01HZX0000000000000000000001"));
    }

    private static SnapshotView sampleSnapshotView() {
        return new SnapshotView(
                "01HZX0000000000000000000000",
                "01HZX0000000000000000000001",
                "MILESTONE",
                "checkpoint",
                Instant.parse("2026-03-21T00:00:00Z")
        );
    }
}
