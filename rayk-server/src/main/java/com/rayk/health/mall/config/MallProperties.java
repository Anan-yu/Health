package com.rayk.health.mall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rayk.mall")
public record MallProperties(boolean enabled, boolean paymentEnabled, int orderExpireMinutes) {
    public MallProperties {
        orderExpireMinutes = Math.max(5, Math.min(orderExpireMinutes <= 0 ? 30 : orderExpireMinutes, 1440));
    }
}
