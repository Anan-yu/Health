package com.rayk.health.followup.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FollowupTaskVo(
        String id,
        String patientId,
        String title,
        String content,
        LocalDate dueDate,
        String status,
        String feedback,
        LocalDateTime completedAt) {}

