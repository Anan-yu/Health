package com.rayk.health.assistant.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HealthTreeHoleFeedbackVo(
        boolean canGenerate,
        boolean hasFeedback,
        int recordedDays,
        int entryCount,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        LocalDate nextFeedbackDate,
        LocalDate feedbackPeriodStart,
        LocalDate feedbackPeriodEnd,
        String content,
        String riskLevel,
        String recommendedAction,
        String model,
        LocalDateTime generatedAt,
        String disclaimer) {}
