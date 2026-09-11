package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record GoldBeanLedgerVo(
        String id,
        String bucket,
        String direction,
        BigDecimal amount,
        String eventType,
        String description,
        LocalDateTime createdAt) {}
