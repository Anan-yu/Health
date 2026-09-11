package com.rayk.health.platform.vo;

import java.math.BigDecimal;

public record PlatformGoldRegionProfitDistributionVo(
        String recipientUserId,
        String recipientName,
        String recipientType,
        int ratePercent,
        BigDecimal amount) {}
