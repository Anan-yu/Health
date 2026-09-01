package com.rayk.health.security.wechat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/** Resolves a one-time getPhoneNumber code with WeChat. Access tokens are cached in Redis. */
@Component
public class WeChatPhoneNumberClient {
    private static final Logger log = LoggerFactory.getLogger(WeChatPhoneNumberClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String ACCESS_TOKEN_KEY_PREFIX = "rayk:wechat:access-token:";

    private final WeChatProperties properties;
    private final WebClient webClient;
    private final StringRedisTemplate redisTemplate;

    public WeChatPhoneNumberClient(
            WeChatProperties properties, WebClient.Builder builder, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.webClient = builder.build();
        this.redisTemplate = redisTemplate;
    }

    public String resolve(String phoneCode) {
        return resolve(phoneCode, false);
    }

    /**
     * Resolves a real WeChat phone credential. The development profile may
     * still enable mock login for H5/debug accounts, but a request carrying a
     * getPhoneNumber code must be verified against WeChat so that a saved
     * doctor or platform-admin phone can be matched correctly.
     */
    public String resolveReal(String phoneCode) {
        return resolve(phoneCode, true);
    }

    private String resolve(String phoneCode, boolean forceReal) {
        if (properties.mockEnabled() && !forceReal) {
            return PhoneIdentity.normalize(
                    StringUtils.hasText(properties.mockPhoneNumber())
                            ? properties.mockPhoneNumber()
                            : "13800000005");
        }
        if (!StringUtils.hasText(phoneCode)
                || !StringUtils.hasText(properties.appId())
                || !StringUtils.hasText(properties.secret())
                || !StringUtils.hasText(properties.accessTokenUrl())
                || !StringUtils.hasText(properties.phoneNumberUrl())) {
            log.warn(
                    "WeChat phone authorization is not configured: hasPhoneCode={}, hasAppId={}, "
                            + "hasSecret={}, hasAccessTokenUrl={}, hasPhoneNumberUrl={}",
                    StringUtils.hasText(phoneCode),
                    StringUtils.hasText(properties.appId()),
                    StringUtils.hasText(properties.secret()),
                    StringUtils.hasText(properties.accessTokenUrl()),
                    StringUtils.hasText(properties.phoneNumberUrl()));
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }

        // A cached token can survive an app-secret rotation or a separate
        // development/production deployment. If WeChat rejects it as invalid,
        // delete it and retry the phone request once with a fresh token.
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String accessToken = accessToken(attempt > 0);
                PhoneHttpResponse httpResponse = requestPhoneNumber(accessToken, phoneCode);
                PhoneNumberResponse response = parsePhoneResponse(httpResponse);
                Integer errorCode = response == null ? null : response.errorCode();
                String errorMessage = response == null ? null : response.errorMessage();
                boolean hasPhoneInfo = response != null && response.phoneInfo() != null;
                log.debug(
                        "WeChat phone authorization response: httpStatus={}, errcode={}, errmsg={}, "
                                + "hasPhoneInfo={}, requestId={}, appIdHash={}",
                        httpResponse.status(),
                        errorCode,
                        errorMessage,
                        hasPhoneInfo,
                        safeRequestId(httpResponse.requestId()),
                        appIdHash());

                if (isInvalidAccessToken(errorCode, httpResponse.status())) {
                    invalidateAccessToken();
                    if (attempt == 0) {
                        log.warn(
                                "WeChat phone authorization rejected the cached access token; "
                                        + "refreshing and retrying once: httpStatus={}, errcode={}, "
                                        + "errmsg={}, requestId={}, appIdHash={}",
                                httpResponse.status(),
                                errorCode,
                                errorMessage,
                                safeRequestId(httpResponse.requestId()),
                                appIdHash());
                        continue;
                    }
                }

                if (!isSuccessful(httpResponse.status(), errorCode)) {
                    log.warn(
                            "WeChat phone authorization failed: httpStatus={}, errcode={}, errmsg={}, "
                                    + "hasPhoneInfo={}, requestId={}, appIdHash={}",
                            httpResponse.status(),
                            errorCode,
                            errorMessage,
                            hasPhoneInfo,
                            safeRequestId(httpResponse.requestId()),
                            appIdHash());
                    throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
                }

                String phone =
                        response == null || response.phoneInfo() == null
                                ? null
                                : response.phoneInfo().purePhoneNumber();
                if (!StringUtils.hasText(phone)) {
                    log.warn(
                            "WeChat phone authorization returned no usable phone number: "
                                    + "httpStatus={}, errcode={}, errmsg={}, requestId={}, appIdHash={}",
                            httpResponse.status(),
                            errorCode,
                            errorMessage,
                            safeRequestId(httpResponse.requestId()),
                            appIdHash());
                    throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
                }
                return PhoneIdentity.normalize(phone);
            } catch (BusinessException | IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
            } catch (RuntimeException exception) {
                log.warn(
                        "WeChat phone authorization request failed: exceptionType={}, "
                                + "appIdHash={}",
                        exception.getClass().getName(),
                        appIdHash());
                throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
            }
        }
        throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
    }

    private PhoneHttpResponse requestPhoneNumber(String accessToken, String phoneCode) {
        URI uri =
                UriComponentsBuilder.fromUriString(properties.phoneNumberUrl())
                        .queryParam("access_token", accessToken)
                        .build()
                        .toUri();
        try {
            ResponseEntity<String> response =
                    webClient
                            .post()
                            .uri(uri)
                            .bodyValue(new PhoneNumberRequest(phoneCode))
                            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
                            .block(REQUEST_TIMEOUT);
            if (response == null) {
                return new PhoneHttpResponse(0, null, "");
            }
            return new PhoneHttpResponse(
                    response.getStatusCode().value(),
                    firstHeader(response, "X-Request-Id", "Request-Id"),
                    response.getBody());
        } catch (RuntimeException exception) {
            log.warn(
                    "WeChat phone authorization HTTP request failed: exceptionType={}, appIdHash={}",
                    exception.getClass().getName(),
                    appIdHash());
            throw exception;
        }
    }

    private PhoneNumberResponse parsePhoneResponse(PhoneHttpResponse httpResponse) {
        if (!StringUtils.hasText(httpResponse.body())) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(httpResponse.body(), PhoneNumberResponse.class);
        } catch (JsonProcessingException exception) {
            log.warn(
                    "WeChat phone authorization returned an unreadable response: httpStatus={}, "
                            + "requestId={}, bodyUnavailable=true, appIdHash={}",
                    httpResponse.status(),
                    safeRequestId(httpResponse.requestId()),
                    appIdHash());
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
    }

    private String accessToken(boolean forceRefresh) {
        String cacheKey = accessTokenKey();
        if (!forceRefresh) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (StringUtils.hasText(cached)) {
                    return cached;
                }
            } catch (RuntimeException exception) {
                // Redis is only a cache. A temporary cache failure should not
                // prevent a direct token request from succeeding.
                log.warn(
                        "WeChat access-token cache read failed: exceptionType={}, appIdHash={}",
                        exception.getClass().getName(),
                        appIdHash());
            }
        } else {
            deleteCachedAccessToken(cacheKey);
        }

        URI uri =
                UriComponentsBuilder.fromUriString(properties.accessTokenUrl())
                        .queryParam("grant_type", "client_credential")
                        .queryParam("appid", properties.appId())
                        .queryParam("secret", properties.secret())
                        .build()
                        .toUri();
        ResponseEntity<String> httpResponse;
        try {
            httpResponse =
                    webClient
                            .get()
                            .uri(uri)
                            .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
                            .block(REQUEST_TIMEOUT);
        } catch (RuntimeException exception) {
            log.warn(
                    "WeChat access-token HTTP request failed: exceptionType={}, appIdHash={}",
                    exception.getClass().getName(),
                    appIdHash());
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }

        int status = httpResponse == null ? 0 : httpResponse.getStatusCode().value();
        String requestId =
                httpResponse == null ? null : firstHeader(httpResponse, "X-Request-Id", "Request-Id");
        String body = httpResponse == null ? null : httpResponse.getBody();
        AccessTokenResponse response = parseAccessTokenResponse(body, status, requestId);
        Integer errorCode = response == null ? null : response.errorCode();
        String errorMessage = response == null ? null : response.errorMessage();
        boolean hasAccessToken = response != null && StringUtils.hasText(response.accessToken());
        log.debug(
                "WeChat access-token response: httpStatus={}, errcode={}, errmsg={}, "
                        + "hasAccessToken={}, requestId={}, appIdHash={}",
                status,
                errorCode,
                errorMessage,
                hasAccessToken,
                safeRequestId(requestId),
                appIdHash());
        if (!isSuccessful(status, errorCode) || !hasAccessToken) {
            log.warn(
                    "WeChat access-token request failed: httpStatus={}, errcode={}, errmsg={}, "
                            + "hasAccessToken={}, requestId={}, appIdHash={}",
                    status,
                    errorCode,
                    errorMessage,
                    hasAccessToken,
                    safeRequestId(requestId),
                    appIdHash());
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }

        long ttl = response.expiresIn() == null ? 7000L : Math.max(60L, response.expiresIn() - 120L);
        try {
            redisTemplate.opsForValue().set(cacheKey, response.accessToken(), Duration.ofSeconds(ttl));
        } catch (RuntimeException exception) {
            log.warn(
                    "WeChat access-token cache write failed; continuing without cache: "
                            + "exceptionType={}, appIdHash={}",
                    exception.getClass().getName(),
                    appIdHash());
        }
        return response.accessToken();
    }

    private AccessTokenResponse parseAccessTokenResponse(String body, int status, String requestId) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(body, AccessTokenResponse.class);
        } catch (JsonProcessingException exception) {
            log.warn(
                    "WeChat access-token response was unreadable: httpStatus={}, requestId={}, "
                            + "bodyUnavailable=true, appIdHash={}",
                    status,
                    safeRequestId(requestId),
                    appIdHash());
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
    }

    private void invalidateAccessToken() {
        deleteCachedAccessToken(accessTokenKey());
    }

    private void deleteCachedAccessToken(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
        } catch (RuntimeException exception) {
            log.warn(
                    "WeChat access-token cache invalidation failed: exceptionType={}, appIdHash={}",
                    exception.getClass().getName(),
                    appIdHash());
        }
    }

    private String accessTokenKey() {
        return ACCESS_TOKEN_KEY_PREFIX + appIdHash();
    }

    private String appIdHash() {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            StringUtils.hasText(properties.appId())
                                                    ? properties.appId().getBytes(StandardCharsets.UTF_8)
                                                    : new byte[0]))
                    .substring(0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean isSuccessful(int status, Integer errorCode) {
        return status >= 200 && status < 300 && (errorCode == null || errorCode == 0);
    }

    private static boolean isInvalidAccessToken(Integer errorCode, int status) {
        return status == 401
                || status == 403
                || errorCode != null
                        && (errorCode == 40001 || errorCode == 40014 || errorCode == 42001);
    }

    private static String firstHeader(ResponseEntity<?> response, String... names) {
        for (String name : names) {
            String value = response.getHeaders().getFirst(name);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static String safeRequestId(String requestId) {
        return StringUtils.hasText(requestId) ? requestId : "unavailable";
    }

    private record PhoneNumberRequest(String code) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhoneNumberResponse(
            @JsonProperty("phone_info") PhoneInfo phoneInfo,
            @JsonProperty("errcode") Integer errorCode,
            @JsonProperty("errmsg") String errorMessage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhoneInfo(@JsonProperty("purePhoneNumber") String purePhoneNumber) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AccessTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("errcode") Integer errorCode,
            @JsonProperty("errmsg") String errorMessage) {}

    private record PhoneHttpResponse(int status, String requestId, String body) {}
}
