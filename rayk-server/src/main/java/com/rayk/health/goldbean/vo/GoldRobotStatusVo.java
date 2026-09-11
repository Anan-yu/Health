package com.rayk.health.goldbean.vo;

import java.math.BigDecimal;

public record GoldRobotStatusVo(
        boolean enabled,
        boolean registered,
        boolean canRedeem,
        boolean groupConfigured,
        BigDecimal costGoldBeans,
        BigDecimal digitalBankBalance,
        long redeemedCount,
        String entitlementName) {}
