package com.rayk.health.assessment.dto;

import jakarta.validation.constraints.NotNull;

public record CreateAssessmentRequest(@NotNull Long reportId) {}

