package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record GoldBeanTradeVo(
        String tradeNo,
        String listingId,
        String bucket,
        BigDecimal quantity,
        long unitPriceCent,
        long totalAmount,
        long paymentAmount,
        String status,
        boolean paymentEnabled,
        LocalDateTime createdAt) {}
