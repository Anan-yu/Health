package com.rayk.health.assessment.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record AssessmentVo(
        String id,
        String reportId,
        String patientId,
        String modelVersion,
        String status,
        String overallRiskLevel,
        JsonNode results,
        String disclaimer,
        LocalDateTime createdAt) {}

