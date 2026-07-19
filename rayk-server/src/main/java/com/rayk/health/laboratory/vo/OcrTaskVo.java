package com.rayk.health.laboratory.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OcrTaskVo(
        String id,
        String reportId,
        String fileId,
        String taskCode,
        String status,
        String engine,
        BigDecimal confidence,
        int attemptCount,
        int indicatorCount,
        List<String> warnings,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt) {}

