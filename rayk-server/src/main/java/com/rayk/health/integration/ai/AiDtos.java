package com.rayk.health.integration.ai;

import java.math.BigDecimal;
import java.util.List;

public final class AiDtos {
    private AiDtos() {}

    public record EvaluateRequest(
            String taskId, String patientId, List<Indicator> indicators, List<String> modelCodes) {
        public EvaluateRequest(String taskId, String patientId, List<Indicator> indicators) {
            this(taskId, patientId, indicators, null);
        }
    }

    public record OcrRecognizeRequest(
            String taskId,
            String fileId,
            String objectName,
            String mimeType,
            String downloadUrl) {}

    public record Indicator(
            String code,
            String name,
            BigDecimal value,
            String unit,
            BigDecimal referenceLow,
            BigDecimal referenceHigh) {}

    public record OcrRecognizeData(
            String engine,
            String status,
            BigDecimal confidence,
            List<Indicator> indicators,
            List<String> rawLines,
            List<String> warnings) {}

    public record ReportGenerateRequest(
            String assessmentId,
            String patientDisplayName,
            String reportNo,
            String publishedAt,
            String doctorOpinion,
            List<Indicator> indicators,
            List<ModelResult> results) {}

    public record ReportGenerateData(
            String title,
            String summary,
            List<String> sections,
            String disclaimer,
            String pdfBase64) {}

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
