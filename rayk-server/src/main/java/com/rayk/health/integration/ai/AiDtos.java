package com.rayk.health.integration.ai;

import java.math.BigDecimal;
import java.util.List;

public final class AiDtos {
    private AiDtos() {}

    public record EvaluateRequest(String taskId, String patientId, List<Indicator> indicators) {}

    public record Indicator(
            String code,
            String name,
            BigDecimal value,
            String unit,
            BigDecimal referenceLow,
            BigDecimal referenceHigh) {}

    public record ApiEnvelope<T>(int code, String message, String requestId, long timestamp, T data) {}

    public record AssessmentData(
            String taskId,
            String modelVersion,
            String status,
            String disclaimer,
            List<ModelResult> results) {}

    public record ModelResult(
            String modelCode,
            String modelName,
            int score,
            String riskLevel,
            List<String> evidence,
            List<String> missingIndicators,
            List<String> recommendations) {}
}

