package com.rayk.health.platform.vo;

import java.time.LocalDateTime;

public record PlatformGoldBeanInviteVo(
        String id,
        String codeMasked,
        String status,
        String boundPhoneMasked,
        String reservedOrderNo,
        String consumedOrderNo,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime consumedAt,
        LocalDateTime revokedAt) {}
