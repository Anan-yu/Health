package com.rayk.health.security.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import java.net.URI;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WeChatCode2SessionClient {
    private final WeChatProperties properties;
    private final WebClient webClient;

    public WeChatCode2SessionClient(WeChatProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.build();
    }

    public WeChatSessionIdentity exchange(String code) {
        if (properties.mockEnabled()) {
            String openid =
                    StringUtils.hasText(properties.mockOpenid())
                            ? properties.mockOpenid()
                            : "rayk-development-openid";
            String appId =
                    StringUtils.hasText(properties.appId())
                            ? properties.appId()
                            : "rayk-development-appid";
            return new WeChatSessionIdentity(appId, openid, null);
        }
        if (!StringUtils.hasText(properties.appId())
                || !StringUtils.hasText(properties.secret())
                || !StringUtils.hasText(properties.code2SessionUrl())) {
            throw new BusinessException(ErrorCode.WECHAT_NOT_CONFIGURED);
        }
        try {
            URI uri =
                    UriComponentsBuilder.fromUriString(properties.code2SessionUrl())
                            .queryParam("appid", properties.appId())
                            .queryParam("secret", properties.secret())
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build()
                            .toUri();
            Code2SessionResponse response =
                    webClient
                            .get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(Code2SessionResponse.class)
                            .block(Duration.ofSeconds(10));
            if (response == null
                    || response.errorCode() != null && response.errorCode() != 0
                    || !StringUtils.hasText(response.openid())) {
                throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED);
            }
            return new WeChatSessionIdentity(properties.appId(), response.openid(), response.unionid());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED);
        }
    }

    private record Code2SessionResponse(
            String openid,
            String unionid,
            @JsonProperty("session_key") String sessionKey,
            @JsonProperty("errcode") Integer errorCode,
            @JsonProperty("errmsg") String errorMessage) {}
}
