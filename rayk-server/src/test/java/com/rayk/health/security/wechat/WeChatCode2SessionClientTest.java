package com.rayk.health.security.wechat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class WeChatCode2SessionClientTest {
    @Test
    void returnsConfiguredDevelopmentIdentityWhenMockIsEnabled() {
        WeChatProperties properties =
                new WeChatProperties(
                        "dev-appid",
                        "",
                        "https://example.invalid",
                        "https://example.invalid/token",
                        "https://example.invalid/phone",
                        true,
                        "dev-openid",
                        "13800000005",
                        20001L,
                        "customer");
        WeChatCode2SessionClient client =
                new WeChatCode2SessionClient(properties, WebClient.builder());

        WeChatSessionIdentity identity = client.exchange("temporary-code");

        assertThat(identity.appId()).isEqualTo("dev-appid");
        assertThat(identity.openid()).isEqualTo("dev-openid");
        assertThat(identity.unionid()).isNull();
    }

    @Test
    void rejectsRealExchangeWhenCredentialsAreMissing() {
        WeChatProperties properties =
                new WeChatProperties(
                        "",
                        "",
                        "https://example.invalid",
                        "https://example.invalid/token",
                        "https://example.invalid/phone",
                        false,
                        "",
                        "",
                        20001L,
                        "");
        WeChatCode2SessionClient client =
                new WeChatCode2SessionClient(properties, WebClient.builder());

        assertThatThrownBy(() -> client.exchange("temporary-code"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.WECHAT_NOT_CONFIGURED);
    }
}
