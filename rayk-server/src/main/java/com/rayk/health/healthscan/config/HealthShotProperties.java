package com.rayk.health.healthscan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rayk.health-shot")
public record HealthShotProperties(
        boolean enabled,
        String appId,
        String key,
        String apiBaseUrl,
        String uploadPath,
        String environment,
        String pluginProvider,
        String pluginVersion,
        String pluginServerUrl,
        int connectTimeoutSeconds,
        int readTimeoutSeconds) {

    public boolean configured() {
        return enabled
                && appId != null
                && !appId.isBlank()
                && key != null
                && !key.isBlank()
                && apiBaseUrl != null
                && !apiBaseUrl.isBlank();
    }
}

