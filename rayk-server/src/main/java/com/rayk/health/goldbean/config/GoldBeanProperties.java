package com.rayk.health.goldbean.config;

import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Feature-gated settings for the 金豆会员 capability. */
@ConfigurationProperties(prefix = "rayk.gold-bean")
public record GoldBeanProperties(
        boolean enabled,
        boolean developmentMode,
        int initialBeans,
        int dailyRewardBeans,
        int dailyRewardDays,
        /** Backward-compatible name for the referral registration fee, in cents. */
        int registrationFeeCent,
        int platformRegistrationFeeCent,
        int protectionDays,
        int limitedTradePercent,
        boolean paymentEnabled,
        String registrationProductId,
        String referralRegistrationProductId,
        String goldBeanProductId,
        int goldBeanUnitPriceCent,
        int maxPurchaseQuantity,
        int virtualPaymentSurchargePercent,
        String robotGroupName,
        String robotGroupQrImageUrl) {

    /**
     * Backward-compatible constructor for callers that only configure the platform registration
     * product. Referral virtual payment must opt in with its own product id.
     */
    public GoldBeanProperties(
            boolean enabled,
            boolean developmentMode,
            int initialBeans,
            int dailyRewardBeans,
            int dailyRewardDays,
            int registrationFeeCent,
            int platformRegistrationFeeCent,
            int protectionDays,
            int limitedTradePercent,
            boolean paymentEnabled,
            String registrationProductId,
            String goldBeanProductId,
            int goldBeanUnitPriceCent,
            int maxPurchaseQuantity,
            String robotGroupName,
            String robotGroupQrImageUrl) {
        this(
                enabled,
                developmentMode,
                initialBeans,
                dailyRewardBeans,
                dailyRewardDays,
                registrationFeeCent,
                platformRegistrationFeeCent,
                protectionDays,
                limitedTradePercent,
                paymentEnabled,
                registrationProductId,
                "",
                goldBeanProductId,
                goldBeanUnitPriceCent,
                maxPurchaseQuantity,
                12,
                robotGroupName,
                robotGroupQrImageUrl);
    }

    /**
     * Backward-compatible constructor for tests and callers that already pass the referral
     * product id but predate the virtual-payment surcharge setting.
     */
    public GoldBeanProperties(
            boolean enabled,
            boolean developmentMode,
            int initialBeans,
            int dailyRewardBeans,
            int dailyRewardDays,
            int registrationFeeCent,
            int platformRegistrationFeeCent,
            int protectionDays,
            int limitedTradePercent,
            boolean paymentEnabled,
            String registrationProductId,
            String referralRegistrationProductId,
            String goldBeanProductId,
            int goldBeanUnitPriceCent,
            int maxPurchaseQuantity,
            String robotGroupName,
            String robotGroupQrImageUrl) {
        this(
                enabled,
                developmentMode,
                initialBeans,
                dailyRewardBeans,
                dailyRewardDays,
                registrationFeeCent,
                platformRegistrationFeeCent,
                protectionDays,
                limitedTradePercent,
                paymentEnabled,
                registrationProductId,
                referralRegistrationProductId,
                goldBeanProductId,
                goldBeanUnitPriceCent,
                maxPurchaseQuantity,
                12,
                robotGroupName,
                robotGroupQrImageUrl);
    }

    @ConstructorBinding
    public GoldBeanProperties {
        initialBeans = initialBeans <= 0 ? 60 : Math.min(initialBeans, 1_000_000);
        dailyRewardBeans = dailyRewardBeans <= 0 ? 60 : Math.min(dailyRewardBeans, 1_000_000);
        dailyRewardDays = dailyRewardDays <= 0 ? 20 : Math.min(dailyRewardDays, 365);
        registrationFeeCent = registrationFeeCent <= 0 ? 100_000 : registrationFeeCent;
        platformRegistrationFeeCent = platformRegistrationFeeCent <= 0 ? 100_000 : platformRegistrationFeeCent;
        protectionDays = protectionDays <= 0 ? 7 : Math.min(protectionDays, 30);
        limitedTradePercent = limitedTradePercent <= 0 ? 50 : Math.min(limitedTradePercent, 100);
        virtualPaymentSurchargePercent = Math.max(0, Math.min(virtualPaymentSurchargePercent, 100));
        registrationProductId = registrationProductId == null ? "" : registrationProductId.trim();
        referralRegistrationProductId = referralRegistrationProductId == null ? "" : referralRegistrationProductId.trim();
        goldBeanProductId = goldBeanProductId == null ? "" : goldBeanProductId.trim();
        goldBeanUnitPriceCent = goldBeanUnitPriceCent <= 0 ? 100 : Math.min(goldBeanUnitPriceCent, 1_000_000);
        maxPurchaseQuantity = maxPurchaseQuantity <= 0 ? 10_000 : Math.min(maxPurchaseQuantity, 10_000);
        robotGroupName = robotGroupName == null || robotGroupName.isBlank()
                ? "机器人权益服务群"
                : robotGroupName.trim();
        robotGroupQrImageUrl = robotGroupQrImageUrl == null ? "" : robotGroupQrImageUrl.trim();
    }

    /**
     * Calculates the amount charged to the buyer in cents. The result is rounded up to a cent;
     * the stored business amount and any merchant-transfer amount remain unchanged.
     */
    public long surchargedPaymentAmountCent(long baseAmountCent) {
        if (baseAmountCent <= 0 || virtualPaymentSurchargePercent == 0) return baseAmountCent;
        long numerator = Math.multiplyExact(baseAmountCent, 100L + virtualPaymentSurchargePercent);
        long quotient = Math.floorDiv(numerator, 100L);
        return numerator % 100L == 0 ? quotient : Math.addExact(quotient, 1L);
    }

    public int surchargedPaymentAmountCent(int baseAmountCent) {
        return Math.toIntExact(surchargedPaymentAmountCent((long) baseAmountCent));
    }
}
