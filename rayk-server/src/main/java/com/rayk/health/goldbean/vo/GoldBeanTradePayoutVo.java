package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/** Seller-visible settlement state for a gold-bean market trade. */
public record GoldBeanTradePayoutVo(
        String tradeNo,
        BigDecimal quantity,
        long amountCent,
        String bucket,
        String settlementStatus,
        String transferState,
        String merchantId,
        String appId,
        boolean userConfirmationRequired,
        String packageInfo,
        LocalDateTime createdAt,
        LocalDateTime settledAt,
        String failureReason) {}
