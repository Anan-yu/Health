package com.rayk.health.indicator.vo;

import java.time.LocalDateTime;

public record HealthModelConfigVo(
        String id,
        String modelCode,
        String modelName,
        String modelCategory,
        String version,
        String status,
        String configSnapshot,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
