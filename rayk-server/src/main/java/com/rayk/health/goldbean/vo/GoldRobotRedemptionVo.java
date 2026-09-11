package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record GoldRobotRedemptionVo(
        String redemptionId,
        String redemptionNo,
        String entitlementCode,
        String entitlementName,
        BigDecimal goldBeanCost,
        BigDecimal digitalBankBalance,
        String groupName,
        String groupQrImageUrl,
        LocalDateTime redeemedAt) {}
