package com.rayk.health.followup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FollowupFeedbackRequest(@NotBlank @Size(max = 1000) String feedback) {}

