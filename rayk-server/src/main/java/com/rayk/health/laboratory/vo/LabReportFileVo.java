package com.rayk.health.laboratory.vo;

import java.time.LocalDateTime;

public record LabReportFileVo(
        String id,
        String reportId,
        String originalName,
        String mimeType,
        long fileSize,
        String sha256,
        String status,
        String downloadUrl,
        LocalDateTime downloadUrlExpiresAt,
        LocalDateTime createdAt) {}

