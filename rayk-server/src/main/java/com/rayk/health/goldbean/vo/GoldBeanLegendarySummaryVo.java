package com.rayk.health.goldbean.vo;

import java.math.BigDecimal;

public record GoldBeanLegendarySummaryVo(
        boolean eligible,
        boolean registered,
        boolean purchaseEnabled,
        String userId,
        BigDecimal digitalBankBalance,
        int goldBeanUnitPriceCent,
        int maxPurchaseQuantity,
        String message) {}
