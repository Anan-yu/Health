package com.rayk.health.integration.ai;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AiServiceClient {
    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);
    private final WebClient webClient;

    public AiServiceClient(WebClient aiWebClient) {
        this.webClient = aiWebClient;
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
                            .block(Duration.ofSeconds(35));
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
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
                            .block(Duration.ofSeconds(125));
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
}
