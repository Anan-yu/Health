package com.rayk.health.security.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WeChatCode2SessionClient {
    private static final Logger log = LoggerFactory.getLogger(WeChatCode2SessionClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                            .encode()
                            .toUri();
            // The jscode2session endpoint currently returns JSON with Content-Type=text/plain.
            // Reading a typed body directly makes WebClient reject the otherwise valid response
            // before the error payload can be inspected. Read the payload as text and decode it
            // explicitly so both text/plain and application/json responses are supported.
            String responseBody =
                    webClient
                            .get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(10));
            Code2SessionResponse response =
                    StringUtils.hasText(responseBody)
                            ? OBJECT_MAPPER.readValue(responseBody, Code2SessionResponse.class)
                            : null;
            if (response == null
                    || response.errorCode() != null && response.errorCode() != 0
                    || !StringUtils.hasText(response.openid())) {
                log.warn(
                        "WeChat code2session rejected: errcode={}, errmsg={}, hasOpenid={}",
                        response == null ? null : response.errorCode(),
                        response == null ? null : response.errorMessage(),
                        response != null && StringUtils.hasText(response.openid()));
                throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED);
            }
            return new WeChatSessionIdentity(properties.appId(), response.openid(), response.unionid());
        } catch (BusinessException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            log.warn(
                    "WeChat code2session returned an unreadable response: exceptionType={}, bodyUnavailable=true",
                    exception.getClass().getName());
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED);
        } catch (WebClientResponseException exception) {
            log.warn(
                    "WeChat code2session HTTP error: status={}, responseBody={}",
                    exception.getStatusCode().value(),
                    exception.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED);
        } catch (RuntimeException exception) {
            log.warn(
                    "WeChat code2session request failed: exceptionType={}",
                    exception.getClass().getName());
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
