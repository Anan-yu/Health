package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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
        BigDecimal totalBalance,
        BigDecimal digitalBankBalance,
        BigDecimal tradingBalance,
        int tradeLimitPercent,
        int dailyRewardDays,
        int dailyRewardTotalDays,
        int dailyRewardRemainingDays,
        String reminderText,
        LocalDateTime protectionUntil,
        boolean regionOpenAllowed,
        String regionId,
        String regionCity,
        boolean paymentEnabled,
        int registrationFeeCent,
        boolean goldBeanPurchaseEnabled,
        int goldBeanUnitPriceCent,
        int maxPurchaseQuantity,
        int platformRegistrationFeeCent,
        int referralRegistrationFeeCent,
        int virtualPaymentSurchargePercent) {}
