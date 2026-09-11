package com.rayk.health.goldbean.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoldBeanOrderVo(
        String orderNo,
        String orderType,
        String status,
        int amountCent,
        int paymentAmountCent,
        BigDecimal goldBeanQuantity,
        String registrationFeeRecipient,
        boolean paymentEnabled,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        String settlementStatus) {
}
