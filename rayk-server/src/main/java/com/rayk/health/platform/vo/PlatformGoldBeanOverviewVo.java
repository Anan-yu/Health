package com.rayk.health.platform.vo;

import java.util.List;
import java.math.BigDecimal;

public record PlatformGoldBeanOverviewVo(
        boolean enabled,
        boolean developmentMode,
        boolean recordOnly,
        int registrationFeeYuan,
        int initialBeans,
        int dailyRewardBeans,
        int dailyRewardDays,
        int protectionDays,
        int limitedTradePercent,
        String firstRegistrationScope,
        long totalMemberCount,
        long registeredMemberCount,
        long pendingMemberCount,
        long platformFeeRegistrationCount,
        long referrerFeeRegistrationCount,
        BigDecimal totalGoldBeanBalance,
        BigDecimal digitalBankBalance,
        BigDecimal tradingBalance,
        BigDecimal dailyRewardBeansIssued,
        long activeRegionCount,
        List<PlatformGoldBeanLedgerVo> recentLedgers) {}
