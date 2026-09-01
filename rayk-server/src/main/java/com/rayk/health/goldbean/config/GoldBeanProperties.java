package com.rayk.health.goldbean.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Feature-gated settings for the development-only 金豆会员 pilot. */
@ConfigurationProperties(prefix = "rayk.gold-bean")
public record GoldBeanProperties(
        boolean enabled,
        boolean developmentMode,
        int initialBeans,
        int dailyRewardBeans,
        int dailyRewardDays,
        int registrationFeeCent,
        int protectionDays,
        int limitedTradePercent) {

    public GoldBeanProperties {
        initialBeans = initialBeans <= 0 ? 60 : Math.min(initialBeans, 1_000_000);
        dailyRewardBeans = dailyRewardBeans <= 0 ? 60 : Math.min(dailyRewardBeans, 1_000_000);
        dailyRewardDays = dailyRewardDays <= 0 ? 20 : Math.min(dailyRewardDays, 365);
        registrationFeeCent = registrationFeeCent <= 0 ? 99_800 : registrationFeeCent;
        protectionDays = protectionDays <= 0 ? 7 : Math.min(protectionDays, 30);
        limitedTradePercent = limitedTradePercent <= 0 ? 50 : Math.min(limitedTradePercent, 100);
    }
}
