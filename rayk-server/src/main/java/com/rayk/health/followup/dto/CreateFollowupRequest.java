package com.rayk.health.followup.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateFollowupRequest(
        @NotNull Long patientId,
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 1000) String content,
        @NotNull @FutureOrPresent LocalDate dueDate,
        Long assigneeId) {}

