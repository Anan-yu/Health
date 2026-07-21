package com.rayk.health.platform.dto;

import java.util.List;

public record HealthReportArtifactRecoveryData(
        int scanned, int recovered, int skipped, List<Item> items) {
    public record Item(
            String reportId,
            String tenantId,
            String status,
            int versionNo,
            String objectPath) {}
}
