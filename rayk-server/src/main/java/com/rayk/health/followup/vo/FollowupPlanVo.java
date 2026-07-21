package com.rayk.health.followup.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FollowupPlanVo(
        String id,
        String patientId,
        String planName,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        List<FollowupTaskVo> tasks,
        LocalDateTime createdAt) {}
