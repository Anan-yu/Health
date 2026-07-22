package com.rayk.health.security.wechat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rayk.wechat")
public record WeChatProperties(
        String appId,
        String secret,
        String code2SessionUrl,
        String accessTokenUrl,
        String phoneNumberUrl,
        boolean mockEnabled,
        String mockOpenid,
        String mockPhoneNumber,
        long defaultCustomerTenantId,
        String autoBindUsername) {}

