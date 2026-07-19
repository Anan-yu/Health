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
                        "dev-appid", "", "https://example.invalid", true, "dev-openid", "customer");
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
                new WeChatProperties("", "", "https://example.invalid", false, "", "");
        WeChatCode2SessionClient client =
                new WeChatCode2SessionClient(properties, WebClient.builder());

        assertThatThrownBy(() -> client.exchange("temporary-code"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.WECHAT_NOT_CONFIGURED);
    }
}
