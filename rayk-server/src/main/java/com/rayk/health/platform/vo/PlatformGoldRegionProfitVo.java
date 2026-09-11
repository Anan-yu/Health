package com.rayk.health.platform.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record PlatformGoldRegionProfitVo(
        String id,
        String tenantId,
        String tenantName,
        String regionId,
        String city,
        int depth,
        String ownerUserId,
        String ownerName,
        BigDecimal amount,
        BigDecimal ownerAmount,
        BigDecimal retainedAmount,
        String status,
        String idempotencyKey,
        LocalDateTime settledAt,
        List<PlatformGoldRegionProfitDistributionVo> distributions) {}
