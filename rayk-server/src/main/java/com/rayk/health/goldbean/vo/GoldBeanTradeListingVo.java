package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record GoldBeanTradeListingVo(
        String id,
        String bucket,
        String regionCity,
        BigDecimal quantity,
        BigDecimal remainingQuantity,
        long unitPriceCent,
        long totalAmount,
        String status,
        boolean mine,
        LocalDateTime createdAt) {}
