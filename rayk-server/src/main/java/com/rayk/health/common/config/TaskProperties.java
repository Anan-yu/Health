package com.rayk.health.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rayk.task")
public record TaskProperties(
        int processingTimeoutMinutes,
        int pendingTimeoutMinutes,
        long recoveryIntervalMs) {

    public TaskProperties {
        if (processingTimeoutMinutes <= 0) processingTimeoutMinutes = 10;
        if (pendingTimeoutMinutes <= 0) pendingTimeoutMinutes = 2;
        if (recoveryIntervalMs <= 0) recoveryIntervalMs = 60000;
    }
}
