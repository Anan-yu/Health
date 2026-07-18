package com.rayk.health.report.vo;

import java.time.LocalDateTime;

public record HealthReportVo(
        String id,
        String patientId,
        String assessmentId,
        String reportNo,
        String title,
        String status,
        String summary,
        String doctorOpinion,
        String disclaimer,
        LocalDateTime publishedAt) {}

