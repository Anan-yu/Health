package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;

/** A referral reward settlement visible only to the referrer who will receive it. */
public record GoldBeanReferralSettlementVo(
        String orderNo,
        int amountCent,
        String settlementStatus,
        String transferState,
        String merchantId,
        String appId,
        boolean userConfirmationRequired,
        String packageInfo,
        LocalDateTime createdAt,
        LocalDateTime settledAt,
        String failureReason) {}
