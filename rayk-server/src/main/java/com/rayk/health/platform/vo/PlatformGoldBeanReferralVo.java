package com.rayk.health.platform.vo;

import java.time.LocalDateTime;

public record PlatformGoldBeanReferralVo(
        String id,
        String tenantId,
        String tenantName,
        String referrerId,
        String referrerName,
        String referredId,
        String referredName,
        String referralCode,
        int registrationFeeYuan,
        String feeRecipientType,
        String feeRecipientName,
        String status,
        LocalDateTime registeredAt) {}
