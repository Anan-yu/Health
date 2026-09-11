package com.rayk.health.platform.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record PlatformGoldBeanLegendaryVo(
        String id,
        String phoneMasked,
        String status,
        String note,
        String matchedUserId,
        String matchedDisplayName,
        String matchedMemberLevelName,
        String matchedRegistrationStatus,
        BigDecimal digitalBankBalance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
