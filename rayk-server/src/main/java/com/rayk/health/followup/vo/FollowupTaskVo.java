package com.rayk.health.followup.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FollowupTaskVo(
        String id,
        String patientId,
        String patientName,
        String title,
        String content,
        LocalDate dueDate,
        String status,
        String feedback,
        String feedbackDetail,
        LocalDateTime completedAt,
        Integer cycleNo,
        Integer maxCycles,
        Integer completionRate,
        String decision,
        String decisionReason,
        Integer reminderCount,
        LocalDateTime lastRemindedAt) {}

