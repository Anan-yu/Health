package com.rayk.health.healthscan.vo;

public record HealthScanSessionVo(
        String taskId,
        String appId,
        long timestamp,
        String outUserId,
        String sign,
        String serverUrl,
        String pluginProvider,
        String pluginVersion) {}

