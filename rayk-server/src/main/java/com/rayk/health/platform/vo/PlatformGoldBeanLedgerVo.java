package com.rayk.health.platform.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record PlatformGoldBeanLedgerVo(
        String id,
        String tenantId,
        String tenantName,
        String userId,
        String displayName,
        String bucket,
        String direction,
        BigDecimal amount,
        String eventType,
        String description,
        LocalDateTime createdAt) {}
