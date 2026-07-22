package com.rayk.health.security.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import java.net.URI;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/** Resolves a one-time getPhoneNumber code with WeChat. Access tokens are cached in Redis. */
@Component
public class WeChatPhoneNumberClient {
    private static final String ACCESS_TOKEN_KEY = "rayk:wechat:access-token";
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
        if (properties.mockEnabled()) {
            return PhoneIdentity.normalize(
                    StringUtils.hasText(properties.mockPhoneNumber())
                            ? properties.mockPhoneNumber()
                            : "13800000005");
        }
        if (!StringUtils.hasText(phoneCode)
                || !StringUtils.hasText(properties.appId())
                || !StringUtils.hasText(properties.secret())) {
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
        try {
            PhoneNumberResponse response =
                    webClient
                            .post()
                            .uri(
                                    UriComponentsBuilder.fromUriString(properties.phoneNumberUrl())
                                            .queryParam("access_token", accessToken())
                                            .build()
                                            .toUri())
                            .bodyValue(new PhoneNumberRequest(phoneCode))
                            .retrieve()
                            .bodyToMono(PhoneNumberResponse.class)
                            .block(Duration.ofSeconds(10));
            String phone = response == null || response.phoneInfo() == null
                    ? null
                    : response.phoneInfo().purePhoneNumber();
            return PhoneIdentity.normalize(phone);
        } catch (BusinessException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
    }

    private String accessToken() {
        String cached = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        URI uri =
                UriComponentsBuilder.fromUriString(properties.accessTokenUrl())
                        .queryParam("grant_type", "client_credential")
                        .queryParam("appid", properties.appId())
                        .queryParam("secret", properties.secret())
                        .build()
                        .toUri();
        AccessTokenResponse response =
                webClient
                        .get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(AccessTokenResponse.class)
                        .block(Duration.ofSeconds(10));
        if (response == null || !StringUtils.hasText(response.accessToken())) {
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
        long ttl = response.expiresIn() == null ? 7000L : Math.max(60L, response.expiresIn() - 120L);
        redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, response.accessToken(), Duration.ofSeconds(ttl));
        return response.accessToken();
    }

    private record PhoneNumberRequest(String code) {}

    private record PhoneNumberResponse(
            @JsonProperty("phone_info") PhoneInfo phoneInfo,
            @JsonProperty("errcode") Integer errorCode) {}

    private record PhoneInfo(@JsonProperty("purePhoneNumber") String purePhoneNumber) {}

    private record AccessTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn) {}
}
