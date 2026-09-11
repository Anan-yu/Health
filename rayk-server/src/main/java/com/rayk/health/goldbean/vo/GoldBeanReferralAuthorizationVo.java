package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;

/** Safe client view of the referrer's one-time WeChat receive authorization. */
public record GoldBeanReferralAuthorizationVo(
        String state,
        boolean available,
        boolean authorized,
        boolean userConfirmationRequired,
        String merchantId,
        String appId,
        String packageInfo,
        LocalDateTime authorizedAt,
        LocalDateTime lastCheckedAt,
        String failureReason) {}
