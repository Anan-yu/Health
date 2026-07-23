package com.rayk.health.integration.ai;

import java.math.BigDecimal;
import java.util.List;

public final class AiDtos {
    private AiDtos() {}

    public record EvaluateRequest(
      String taskId,
      String patientId,
      List<Indicator> indicators,
      List<String> modelCodes,
      PatientContext patientContext) {
        public EvaluateRequest(String taskId, String patientId, List<Indicator> indicators) {
      this(taskId, patientId, indicators, null, null);
    }

    public EvaluateRequest(
        String taskId, String patientId, List<Indicator> indicators, List<String> modelCodes) {
      this(taskId, patientId, indicators, modelCodes, null);
        }
    }

  public record PatientContext(
      String gender,
      Integer age,
      BigDecimal heightCm,
      BigDecimal weightKg,
      BigDecimal bmi,
      String medicalHistory,
      String familyHistory,
      String diabetesStatus,
      String hypertensionStatus,
      String dyslipidemiaStatus,
      String fattyLiverStatus,
      String smokingStatus,
      String alcoholStatus,
      String exerciseFrequency,
      String sleepQuality,
      BigDecimal sleepHours,
      String stressLevel,
      String moodStatus,
      String fearLevel,
      String dietaryPreference,
      String recentDietaryPattern) {
    public PatientContext(String gender, Integer age) {
      this(gender, age, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
  }

    public record OcrRecognizeRequest(
      String taskId, String fileId, String objectName, String mimeType, String downloadUrl) {}

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
      List<ModelResult> results,
      ComprehensiveInterpretation interpretation,
      PatientContext patientContext) {}

    public record ReportGenerateData(
      String title, String summary, List<String> sections, String disclaimer, String pdfBase64) {}

  public record ApiEnvelope<T>(
      int code, String message, String requestId, long timestamp, T data) {}

    public record AssessmentData(
            String taskId,
            String modelVersion,
            String status,
            String disclaimer,
      List<ModelResult> results,
      ComprehensiveInterpretation interpretation,
      PatientContext patientContext) {}

    public record ModelResult(
            String modelCode,
            String modelName,
      String modelVersion,
      String status,
      Integer score,
            String riskLevel,
      int dataCompleteness,
      String confidence,
            List<String> evidence,
      List<String> supportingIndicators,
            List<String> missingIndicators,
      List<String> recommendations) {
    public ModelResult(
        String modelCode,
        String modelName,
        Integer score,
        String riskLevel,
        List<String> evidence,
        List<String> missingIndicators,
        List<String> recommendations) {
      this(
          modelCode,
          modelName,
          "3.0.0",
          "EVALUATED",
          score,
          riskLevel,
          100,
          "MEDIUM",
          evidence,
          List.of(),
          missingIndicators,
          recommendations);
    }
  }

  public record CrossModelFinding(String title, List<String> indicatorCodes, String explanation) {}

  public record ComprehensiveInterpretation(
      String status,
      String source,
      String model,
      String summary,
      List<String> priorityConcerns,
      List<CrossModelFinding> crossModelFindings,
      List<String> recommendations,
      List<String> missingDataAdvice,
      List<String> followupQuestions,
      List<String> redFlags,
      String uncertainty,
      String disclaimer) {}
}
