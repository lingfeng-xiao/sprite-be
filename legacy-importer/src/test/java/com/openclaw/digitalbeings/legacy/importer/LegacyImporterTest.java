package com.openclaw.digitalbeings.legacy.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyImporterTest {

    @TempDir
    Path tempDir;

    @Test
    void dryRunProducesPlannedDiscoveryAndSummary() throws IOException {
        Path sourceRoot = tempDir.resolve("python-repo");
        Files.createDirectories(sourceRoot.resolve("agents/guan-guan/sessions"));
        Files.createDirectories(sourceRoot.resolve("agents/guan-guan/workspace"));
        Files.createDirectories(sourceRoot.resolve("bridge/runtime"));
        Files.createDirectories(sourceRoot.resolve("memory"));
        Files.createDirectories(sourceRoot.resolve("browser/user-data"));

        Files.writeString(sourceRoot.resolve("agents/guan-guan/sessions/session.jsonl"), "{}\n");
        Files.writeString(sourceRoot.resolve("agents/guan-guan/workspace/notes.md"), "# notes\n");
        Files.writeString(sourceRoot.resolve("bridge/runtime/state.json"), "{}\n");
        Files.writeString(sourceRoot.resolve("memory/today.md"), "# memory\n");
        Files.writeString(sourceRoot.resolve("browser/user-data/deleted.json.deleted.2026-03-21"), "{}\n");
        Files.writeString(sourceRoot.resolve("README.txt"), "legacy importer dry run\n");

        LegacyImportDryRunReport report = new LegacyImporter().dryRun(sourceRoot);

        assertThat(report.sourceRoot()).isEqualTo(sourceRoot.toAbsolutePath().normalize());
        assertThat(report.supportedImportCategories())
                .contains(
                        LegacyImportCategory.BEINGS,
                        LegacyImportCategory.RELATIONSHIPS,
                        LegacyImportCategory.ACCEPTED_REVIEW_ITEMS,
                        LegacyImportCategory.OWNER_PROFILE_FACTS,
                        LegacyImportCategory.SESSIONS_AND_LEASES_METADATA,
                        LegacyImportCategory.SNAPSHOTS
                );
        assertThat(report.plannedSourceDiscovery())
                .extracting(LegacySourceDiscovery::label)
                .contains("repository-root", "agents", "bridge", "memory", "browser");
        assertThat(report.countSummary().discoveredFiles()).isGreaterThanOrEqualTo(6L);
        assertThat(report.countSummary().pythonFiles()).isZero();
        assertThat(report.countSummary().jsonFiles()).isGreaterThanOrEqualTo(1L);
        assertThat(report.countSummary().jsonlFiles()).isGreaterThanOrEqualTo(1L);
        assertThat(report.countSummary().markdownFiles()).isGreaterThanOrEqualTo(2L);
        assertThat(report.countSummary().otherFiles()).isGreaterThanOrEqualTo(1L);
        assertThat(report.anomalies())
                .anySatisfy(anomaly -> assertThat(anomaly).contains("Deleted artifact marker found"));
        assertThat(report.render())
                .contains("plannedSourceDiscovery=")
                .contains("countSummary=")
                .contains("warnings=")
                .contains("anomalies=")
                .contains("supportedImportCategories=");
    }

    @Test
    void dryRunFlagsMissingSourceRoot() {
        Path sourceRoot = tempDir.resolve("missing-python-repo");

        LegacyImportDryRunReport report = new LegacyImporter().dryRun(sourceRoot);

        assertThat(report.anomalies())
                .anySatisfy(anomaly -> assertThat(anomaly).contains("Source root does not exist"));
        assertThat(report.countSummary().discoveredFiles()).isZero();
        assertThat(report.plannedSourceDiscovery())
                .extracting(LegacySourceDiscovery::present)
                .containsOnly(false);
    }

    @Test
    void replayProducesReportWithStatistics() throws IOException {
        Path sourceRoot = tempDir.resolve("python-repo-replay");
        Files.createDirectories(sourceRoot.resolve("agents/guan-guan/sessions"));
        Files.createDirectories(sourceRoot.resolve("agents/guan-guan/workspace"));
        Files.createDirectories(sourceRoot.resolve("bridge/runtime"));
        Files.createDirectories(sourceRoot.resolve("memory"));
        Files.createDirectories(sourceRoot.resolve("browser/user-data"));

        Files.writeString(sourceRoot.resolve("agents/guan-guan/sessions/session.jsonl"), "{}\n");
        Files.writeString(sourceRoot.resolve("agents/guan-guan/workspace/notes.md"), "# notes\n");
        Files.writeString(sourceRoot.resolve("bridge/runtime/state.json"), "{}\n");
        Files.writeString(sourceRoot.resolve("memory/today.md"), "# memory\n");
        Files.writeString(sourceRoot.resolve("browser/user-data/deleted.json.deleted.2026-03-21"), "{}\n");
        Files.writeString(sourceRoot.resolve("README.txt"), "legacy importer replay\n");

        LegacyImportReplayReport report = new LegacyImporter().replay(sourceRoot);

        assertThat(report.sourceRoot()).isEqualTo(sourceRoot.toAbsolutePath().normalize());
        assertThat(report.supportedImportCategories())
                .contains(
                        LegacyImportCategory.BEINGS,
                        LegacyImportCategory.RELATIONSHIPS,
                        LegacyImportCategory.ACCEPTED_REVIEW_ITEMS,
                        LegacyImportCategory.OWNER_PROFILE_FACTS,
                        LegacyImportCategory.SESSIONS_AND_LEASES_METADATA,
                        LegacyImportCategory.SNAPSHOTS
                );
        assertThat(report.countSummary().discoveredFiles()).isGreaterThanOrEqualTo(6L);
        assertThat(report.statistics().beingsCreated()).isZero(); // No actual import yet
        assertThat(report.graphConsistencyChecks()).isNotEmpty();
        assertThat(report.render())
                .contains("LegacyImportReplayReport:")
                .contains("statistics:")
                .contains("graphConsistencyChecks:");
    }

    @Test
    void replayFlagsMissingSourceRoot() {
        Path sourceRoot = tempDir.resolve("missing-python-repo-replay");

        LegacyImportReplayReport report = new LegacyImporter().replay(sourceRoot);

        assertThat(report.anomalies())
                .anySatisfy(anomaly -> assertThat(anomaly).contains("Source root does not exist"));
        assertThat(report.countSummary().discoveredFiles()).isZero();
        assertThat(report.plannedSourceDiscovery())
                .extracting(LegacySourceDiscovery::present)
                .containsOnly(false);
    }
}
