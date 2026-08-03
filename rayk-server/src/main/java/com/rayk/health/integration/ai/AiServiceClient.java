package com.rayk.health.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class AiServiceClient {
    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);
    private final WebClient webClient;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;

    public AiServiceClient(
            WebClient aiWebClient, AiProperties properties, ObjectMapper objectMapper) {
        this.webClient = aiWebClient;
        this.requestTimeout = Duration.ofSeconds(properties.readTimeoutSeconds());
        this.objectMapper = objectMapper;
    }

    public AiDtos.AssessmentData evaluate(AiDtos.EvaluateRequest request) {
        long started = System.nanoTime();
        try {
            AiDtos.ApiEnvelope<AiDtos.AssessmentData> response =
                    webClient
                            .post()
                            .uri("/api/v1/assessments/evaluate")
                            .header("X-Request-Id", MDC.get("requestId"))
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(
                                    new ParameterizedTypeReference<
                                            AiDtos.ApiEnvelope<AiDtos.AssessmentData>>() {})
                            .block(requestTimeout);
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (WebClientResponseException exception) {
            log.warn(
                    "AI service rejected request: status={} validation={}",
                    exception.getStatusCode().value(),
                    validationSummary(exception.getResponseBodyAsString()));
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            log.warn("AI service call failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } finally {
            log.info("AI evaluate call elapsedMs={}", (System.nanoTime() - started) / 1_000_000);
        }
    }

    public AiDtos.OcrRecognizeData recognize(AiDtos.OcrRecognizeRequest request) {
        long started = System.nanoTime();
        try {
            AiDtos.ApiEnvelope<AiDtos.OcrRecognizeData> response =
                    webClient
                            .post()
                            .uri("/api/v1/ocr/recognize")
                            .header("X-Request-Id", MDC.get("requestId"))
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(
                                    new ParameterizedTypeReference<
                                            AiDtos.ApiEnvelope<AiDtos.OcrRecognizeData>>() {})
                            .block(requestTimeout);
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new BusinessException(ErrorCode.OCR_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("OCR service call failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.OCR_SERVICE_UNAVAILABLE);
        } finally {
            log.info("OCR recognize call elapsedMs={}", (System.nanoTime() - started) / 1_000_000);
        }
    }

    public AiDtos.ReportGenerateData generateReport(AiDtos.ReportGenerateRequest request) {
        long started = System.nanoTime();
        try {
            AiDtos.ApiEnvelope<AiDtos.ReportGenerateData> response =
                    webClient
                            .post()
                            .uri("/api/v1/reports/generate")
                            .header("X-Request-Id", MDC.get("requestId"))
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(
                                    new ParameterizedTypeReference<
                                            AiDtos.ApiEnvelope<AiDtos.ReportGenerateData>>() {})
                            .block(requestTimeout);
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("AI report generation call failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } finally {
            log.info(
                    "AI report generation call elapsedMs={}",
                    (System.nanoTime() - started) / 1_000_000);
        }
    }

    public AiDtos.FollowupAdjustmentData adjustFollowup(
            AiDtos.FollowupAdjustmentRequest request) {
        long started = System.nanoTime();
        try {
            AiDtos.ApiEnvelope<AiDtos.FollowupAdjustmentData> response =
                    webClient
                            .post()
                            .uri("/api/v1/followups/adjust")
                            .header("X-Request-Id", MDC.get("requestId"))
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(
                                    new ParameterizedTypeReference<
                                            AiDtos.ApiEnvelope<
                                                    AiDtos.FollowupAdjustmentData>>() {})
                            .block(requestTimeout);
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn(
                    "AI follow-up adjustment call failed: {}",
                    exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } finally {
            log.info(
                    "AI follow-up adjustment call elapsedMs={}",
                    (System.nanoTime() - started) / 1_000_000);
        }
    }

    private String validationSummary(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode detail = root.path("detail");
            if (!detail.isArray()) {
                detail = root.path("data").path("errors");
            }
            if (!detail.isArray()) {
                return "unavailable";
            }
            List<String> issues = new ArrayList<>();
            detail.forEach(
                    issue -> {
                        List<String> location = new ArrayList<>();
                        issue.path("loc").forEach(part -> location.add(part.asText()));
                        issues.add(
                                String.join(".", location)
                                        + ":"
                                        + issue.path("type").asText("validation_error"));
                    });
            return issues.isEmpty() ? "unavailable" : String.join(",", issues);
        } catch (Exception ignored) {
            return "unavailable";
        }
    }
}
