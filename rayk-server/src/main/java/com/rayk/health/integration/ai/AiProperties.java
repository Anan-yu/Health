package com.rayk.health.integration.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rayk.ai")
public record AiProperties(String baseUrl, int connectTimeoutSeconds, int readTimeoutSeconds) {}

