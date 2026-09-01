package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;

public record GoldBeanLedgerVo(
        String id,
        String bucket,
        String direction,
        long amount,
        String eventType,
        String description,
        LocalDateTime createdAt) {}
