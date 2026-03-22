package com.openclaw.digitalbeings.legacy.importer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Report produced by a full import replay.
 * Captures import statistics, anomalies encountered, and graph consistency checks.
 */
public record LegacyImportReplayReport(
        Path sourceRoot,
        LegacyImportPlan plan,
        List<LegacySourceDiscovery> plannedSourceDiscovery,
        LegacyImportCountSummary countSummary,
        ImportStatistics statistics,
        List<String> warnings,
        List<String> anomalies,
        List<String> graphConsistencyChecks,
        List<LegacyImportCategory> supportedImportCategories
) {

    public record ImportStatistics(
            long beingsCreated,
            long identityFacetsCreated,
            long relationshipsCreated,
            long reviewItemsCreated,
            long ownerProfileFactsCreated,
            long sessionsCreated,
            long leasesCreated,
            long snapshotsCreated,
            long parseFailures,
            long mappingFailures,
            long graphViolations
    ) {}

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("LegacyImportReplayReport:\n");
        sb.append("  sourceRoot: ").append(sourceRoot).append("\n");
        sb.append("  plannedSourceDiscovery: ").append(plannedSourceDiscovery).append("\n");
        sb.append("  countSummary: ").append(countSummary).append("\n");
        sb.append("  statistics: ").append(statistics).append("\n");
        sb.append("  warnings: ").append(warnings).append("\n");
        sb.append("  anomalies: ").append(anomalies).append("\n");
        sb.append("  graphConsistencyChecks: ").append(graphConsistencyChecks).append("\n");
        sb.append("  supportedImportCategories: ").append(supportedImportCategories).append("\n");
        return sb.toString();
    }
}
