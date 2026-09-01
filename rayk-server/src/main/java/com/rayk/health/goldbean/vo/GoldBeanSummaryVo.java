package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;

public record GoldBeanSummaryVo(
        boolean enabled,
        String userId,
        String referralCode,
        String memberLevel,
        String memberLevelName,
        String historicalLevel,
        String historicalLevelName,
        int directReferralCount,
        String nextLevel,
        String nextLevelName,
        int nextLevelThreshold,
        String registrationFeeStatus,
        String registrationFeeRecipient,
        long totalBalance,
        long digitalBankBalance,
        long tradingBalance,
        int tradeLimitPercent,
        int dailyRewardDays,
        int dailyRewardTotalDays,
        int dailyRewardRemainingDays,
        String reminderText,
        LocalDateTime protectionUntil,
        boolean regionOpenAllowed,
        String regionId,
        String regionCity) {}
