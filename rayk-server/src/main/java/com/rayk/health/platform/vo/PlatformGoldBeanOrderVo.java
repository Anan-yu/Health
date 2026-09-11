package com.rayk.health.platform.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record PlatformGoldBeanOrderVo(
        String orderNo,
        String tenantId,
        String tenantName,
        String customerId,
        String customerName,
        String orderType,
        String status,
        int amountCent,
        int paymentAmountCent,
        BigDecimal goldBeanQuantity,
        String feeRecipientName,
        String paymentChannel,
        String transactionIdMasked,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        String settlementStatus,
        String settlementFailureReason) {}
