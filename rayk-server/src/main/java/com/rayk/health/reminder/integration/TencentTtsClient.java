package com.rayk.health.reminder.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.reminder.config.TencentTtsProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TencentTtsClient {
    private static final Logger log = LoggerFactory.getLogger(TencentTtsClient.class);
    private static final String HOST = "tts.tencentcloudapi.com";
    private static final String SERVICE = "tts";
    private static final String ACTION = "TextToVoice";
    private static final String VERSION = "2019-08-23";
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TencentTtsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TencentTtsClient(TencentTtsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean available() {
        return properties.configured();
    }

    public byte[] synthesize(String text, int voiceType) {
        if (!properties.configured()) {
            throw new BusinessException(ErrorCode.VOICE_SERVICE_UNAVAILABLE);
        }
        try {
            String payload = objectMapper.writeValueAsString(payload(text, voiceType));
            long timestamp = Instant.now().getEpochSecond();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + HOST))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", authorization(payload, timestamp))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("X-TC-Action", ACTION)
                    .header("X-TC-Version", VERSION)
                    .header("X-TC-Timestamp", Long.toString(timestamp))
                    .header("X-TC-Region", properties.region())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body()).path("Response");
            JsonNode error = root.path("Error");
            if (response.statusCode() >= 400 || !error.isMissingNode()) {
                String providerCode = error.path("Code").asText("UNKNOWN");
                log.warn(
                        "Tencent TTS failed status={} code={} requestId={}",
                        response.statusCode(),
                        providerCode,
                        root.path("RequestId").asText("unknown"));
                throw providerException(providerCode);
            }
            String audio = root.path("Audio").asText();
            if (audio.isBlank()) {
                throw new BusinessException(ErrorCode.VOICE_SERVICE_UNAVAILABLE);
            }
            return java.util.Base64.getDecoder().decode(audio);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Tencent TTS call failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.VOICE_SERVICE_UNAVAILABLE);
        }
    }

    private BusinessException providerException(String providerCode) {
        if ("UnsupportedOperation.PkgExhausted".equals(providerCode)) {
            return new BusinessException(ErrorCode.VOICE_SERVICE_QUOTA_EXHAUSTED);
        }
        return new BusinessException(ErrorCode.VOICE_SERVICE_UNAVAILABLE);
    }

    private Map<String, Object> payload(String text, int voiceType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("Text", text);
        payload.put("SessionId", java.util.UUID.randomUUID().toString());
        payload.put("Volume", properties.volume());
        payload.put("Speed", properties.speed());
        payload.put("ProjectId", 0);
        payload.put("ModelType", 1);
        payload.put("VoiceType", voiceType);
        payload.put("PrimaryLanguage", 1);
        payload.put("SampleRate", properties.sampleRate());
        payload.put("Codec", "mp3");
        return payload;
    }

    private String authorization(String payload, long timestamp) throws Exception {
        String canonicalHeaders =
                "content-type:application/json; charset=utf-8\nhost:" + HOST + "\nx-tc-action:"
                        + ACTION.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String canonicalRequest =
                "POST\n/\n\n"
                        + canonicalHeaders
                        + "\n"
                        + signedHeaders
                        + "\n"
                        + sha256(payload.getBytes(StandardCharsets.UTF_8));
        String date = Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC).format(DATE_FORMAT);
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign =
                ALGORITHM
                        + "\n"
                        + timestamp
                        + "\n"
                        + credentialScope
                        + "\n"
                        + sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        byte[] secretDate = hmac(("TC3" + properties.secretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac(secretDate, SERVICE);
        byte[] secretSigning = hmac(secretService, "tc3_request");
        String signature = HexFormat.of().formatHex(hmac(secretSigning, stringToSign));
        return ALGORITHM
                + " Credential="
                + properties.secretId()
                + "/"
                + credentialScope
                + ", SignedHeaders="
                + signedHeaders
                + ", Signature="
                + signature;
    }

    private static byte[] hmac(byte[] key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }
}
