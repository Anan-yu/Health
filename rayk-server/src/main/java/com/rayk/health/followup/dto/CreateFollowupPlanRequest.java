package com.rayk.health.followup.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateFollowupPlanRequest(
        @NotNull Long patientId,
        @NotBlank @Size(max = 100) String planName,
        @NotNull @FutureOrPresent LocalDate startDate,
        @Min(1) int intervalDays,
        @Min(1) int totalOccurrences,
        @NotBlank @Size(max = 100) String title,
        @Size(max = 1000) String content,
        Long assigneeId) {}
