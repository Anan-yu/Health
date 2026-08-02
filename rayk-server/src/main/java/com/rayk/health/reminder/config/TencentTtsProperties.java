package com.rayk.health.reminder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rayk.tts")
public record TencentTtsProperties(
        boolean enabled,
        String appId,
        String secretId,
        String secretKey,
        String region,
        int femaleVoiceType,
        int maleVoiceType,
        float speed,
        float volume,
        int sampleRate) {
    public boolean configured() {
        return enabled
                && appId != null
                && !appId.isBlank()
                && secretId != null
                && !secretId.isBlank()
                && secretKey != null
                && !secretKey.isBlank();
    }
}
