package com.rayk.health.platform.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record PlatformGoldBeanAccountVo(
        String accountId,
        String tenantId,
        String tenantName,
        String userId,
        String displayName,
        String phoneMasked,
        String memberLevel,
        String memberLevelName,
        String historicalLevel,
        String historicalLevelName,
        int directReferralCount,
        String referrerId,
        String referrerName,
        String referralCode,
        String registrationFeeStatus,
        String feeRecipientType,
        String feeRecipientName,
        int registrationFeeYuan,
        LocalDateTime registrationFeePaidAt,
        BigDecimal totalBalance,
        BigDecimal digitalBankBalance,
        BigDecimal tradingBalance,
        int tradeLimitPercent,
        int dailyRewardDays,
        int dailyRewardTotalDays,
        int dailyRewardRemainingDays,
        LocalDateTime protectionUntil,
        String city,
        String status,
        LocalDateTime createdAt) {}
