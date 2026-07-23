package com.rayk.health.platform.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read-only platform view of health follow-up progress. */
public record PlatformFollowupVo(
        String id,
        String patientName,
        String title,
        LocalDate dueDate,
        String status,
        String feedback,
        LocalDateTime completedAt) {}
