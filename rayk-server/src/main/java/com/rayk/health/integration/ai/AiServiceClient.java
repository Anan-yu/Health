package com.rayk.health.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

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
        log.info(
                "AI evaluate request prepared model={} imageCount={}",
                request.model(),
                request.reportImages() == null ? 0 : request.reportImages().size());
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

    public AiDtos.MedicalAssistantData answerMedicalAssistant(
            AiDtos.MedicalAssistantRequest request) {
        long started = System.nanoTime();
        log.info(
                "Medical assistant request prepared model={} messageCount={}",
                request.model(),
                request.messages() == null ? 0 : request.messages().size());
        try {
            AiDtos.ApiEnvelope<AiDtos.MedicalAssistantData> response =
                    webClient
                            .post()
                            .uri("/api/v1/medical-assistant/answer")
                            .header("X-Request-Id", MDC.get("requestId"))
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(
                                    new ParameterizedTypeReference<
                                            AiDtos.ApiEnvelope<AiDtos.MedicalAssistantData>>() {})
                            .block(requestTimeout);
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (WebClientResponseException exception) {
            log.warn(
                    "Medical assistant rejected request: status={} validation={}",
                    exception.getStatusCode().value(),
                    validationSummary(exception.getResponseBodyAsString()));
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            log.warn(
                    "Medical assistant call failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } finally {
            log.info(
                    "Medical assistant call elapsedMs={}",
                    (System.nanoTime() - started) / 1_000_000);
        }
    }

    /** Relay complete SSE data frames from Python without buffering the model reply. */
    public Flux<String> streamMedicalAssistant(AiDtos.MedicalAssistantRequest request) {
        long started = System.nanoTime();
        Flux<String> response =
                webClient
                        .post()
                        .uri("/api/v1/medical-assistant/answer/stream")
                        .header("X-Request-Id", MDC.get("requestId"))
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToFlux(
                                new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                        .map(ServerSentEvent::data)
                        .filter(data -> data != null && !data.isBlank());
        return response
                .timeout(requestTimeout)
                .doOnError(
                        exception ->
                                log.warn(
                                        "Medical assistant stream failed: {}",
                                        exception.getClass().getSimpleName()))
                .doFinally(
                        signal ->
                                log.info(
                                        "Medical assistant stream elapsedMs={} signal={}",
                                        (System.nanoTime() - started) / 1_000_000,
                                        signal));
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

    private Flux<String> extractSseData(Flux<String> chunks) {
        return Flux.<String>defer(
                () -> {
                    StringBuilder buffer = new StringBuilder();
                    return chunks
                            .handle(
                                    (String chunk, SynchronousSink<String> sink) -> {
                                        buffer.append(chunk);
                                        String event;
                                        while ((event = takeSseEvent(buffer)) != null) {
                                            String data = sseData(event);
                                            if (!data.isBlank()) sink.next(data);
                                        }
                                    })
                            .concatWith(
                                    Mono.defer(
                                            () -> {
                                                String data = drainSseData(buffer);
                                                return data == null || data.isBlank()
                                                        ? Mono.empty()
                                                        : Mono.just(data);
                                            }));
                });
    }

    private String takeSseEvent(StringBuilder buffer) {
        int boundary = buffer.indexOf("\n\n");
        if (boundary < 0) return null;
        String event = buffer.substring(0, boundary);
        buffer.delete(0, boundary + 2);
        return event;
    }

    private String drainSseData(StringBuilder buffer) {
        if (buffer.isEmpty()) return null;
        String event = buffer.toString();
        buffer.setLength(0);
        return sseData(event);
    }

    private String sseData(String event) {
        StringBuilder data = new StringBuilder();
        for (String line : event.split("\\r?\\n")) {
            if (line.startsWith("data:")) {
                if (data.length() > 0) data.append('\n');
                data.append(line.substring(5).stripLeading());
            }
        }
        return data.toString();
    }
}
