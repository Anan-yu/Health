package com.rayk.health.healthscan.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.healthscan.config.HealthShotProperties;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
public class HealthShotVendorClient {
    private final HealthShotProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public HealthShotVendorClient(HealthShotProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        HttpClient client =
                HttpClient.create()
                        .responseTimeout(
                                Duration.ofSeconds(Math.max(10, properties.readTimeoutSeconds())));
        this.webClient =
                WebClient.builder()
                        .baseUrl(properties.apiBaseUrl() == null ? "http://127.0.0.1" : properties.apiBaseUrl())
                        .clientConnector(new ReactorClientHttpConnector(client))
                        .build();
    }

    public HealthShotVendorResult upload(
            String outUserId, long timestamp, String digest, byte[] video, String originalName) {
        Map<String, Object> signParameters =
                Map.of(
                        "appId", properties.appId(),
                        "outUserId", outUserId,
                        "timestamp", timestamp,
                        "videoDigest", digest);
        String sign = HealthShotSigner.sign(signParameters, properties.key());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("appId", properties.appId());
        body.add("outUserId", outUserId);
        body.add("timestamp", String.valueOf(timestamp));
        body.add("videoDigest", digest);
        body.add("sign", sign);
        body.add(
                "video",
                new ByteArrayResource(video) {
                    @Override
                    public String getFilename() {
                        return originalName == null || originalName.isBlank()
                                ? "health-scan.mp4"
                                : originalName;
                    }
                });

        String response =
                webClient
                        .post()
                        .uri(properties.uploadPath())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(body))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
        return parse(response == null ? "{}" : response);
    }

    HealthShotVendorResult parse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode payload = unwrap(root);
            String code = firstText(root, "code", "status", "errCode", "errorCode");
            String message = firstText(root, "message", "msg", "errMsg", "errorMessage");
            String detectId = firstText(payload, "detectId", "detectionId", "taskId", "id");
            BigDecimal heartRate = firstNumber(payload, "heartRate", "heart_rate");
            BigDecimal hrv =
                    firstNumber(
                            payload,
                            "heartRateVariability",
                            "heart_rate_variability",
                            "hrv");
            BigDecimal oxygen =
                    firstNumber(payload, "oxygenSaturation", "oxygen_saturation", "spo2");
            BigDecimal respiration =
                    firstNumber(payload, "respirationRate", "respiration_rate");
            BigDecimal systolic =
                    firstNumber(
                            payload,
                            "systolicBloodPressure",
                            "systolic_blood_pressure",
                            "sbp");
            BigDecimal diastolic =
                    firstNumber(
                            payload,
                            "diastolicBloodPressure",
                            "diastolic_blood_pressure",
                            "dbp");
            BigDecimal stress = firstNumber(payload, "stressHrv", "stress_hrv", "stress");
            BigDecimal quality =
                    firstNumber(payload, "qualityScore", "quality_score", "confidence");
            boolean hasVitals =
                    heartRate != null
                            || oxygen != null
                            || respiration != null
                            || systolic != null
                            || diastolic != null;
            boolean failed =
                    code != null
                            && !code.isBlank()
                            && !"0".equals(code)
                            && !"200".equals(code)
                            && !"SUCCESS".equalsIgnoreCase(code);
            return new HealthShotVendorResult(
                    code,
                    message,
                    detectId,
                    heartRate,
                    hrv,
                    oxygen,
                    respiration,
                    systolic,
                    diastolic,
                    stress,
                    quality,
                    hasVitals,
                    failed,
                    raw);
        } catch (Exception exception) {
            return new HealthShotVendorResult(
                    "INVALID_RESPONSE",
                    "健康检测服务返回格式暂未适配",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    true,
                    raw);
        }
    }

    private JsonNode unwrap(JsonNode root) {
        JsonNode data = root.path("data");
        if (!data.isMissingNode() && !data.isNull()) {
            JsonNode result = data.path("result");
            return !result.isMissingNode() && !result.isNull() ? result : data;
        }
        JsonNode result = root.path("result");
        return !result.isMissingNode() && !result.isNull() ? result : root;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private BigDecimal firstNumber(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                try {
                    return value.decimalValue();
                } catch (Exception ignored) {
                    try {
                        return new BigDecimal(value.asText().trim());
                    } catch (NumberFormatException ignoredAgain) {
                        // Try the next supported alias.
                    }
                }
            }
        }
        return null;
    }
}

