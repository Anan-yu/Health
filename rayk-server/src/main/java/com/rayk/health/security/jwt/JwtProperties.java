package com.rayk.health.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rayk.jwt")
public record JwtProperties(String secret, long expireSeconds) {}

