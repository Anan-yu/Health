package com.rayk.health.platform.vo;

import java.time.LocalDateTime;

public record AiModelRuntimeConfigVo(
        String id,
        String modelCode,
        String modelName,
        String provider,
        String modelVersion,
        String baseUrl,
        Integer contextLengthTokens,
        Integer maxOutputTokens,
        boolean thinkingSupported,
        boolean thinkingEnabled,
        boolean selected,
        String status,
        LocalDateTime updatedAt) {}
