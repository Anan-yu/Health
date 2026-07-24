package com.rayk.health.followup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FollowupActionFeedback(
        @NotBlank @Size(max = 50) String section,
        @NotBlank @Size(max = 300) String action,
        @NotBlank
                @Pattern(regexp = "COMPLETED|PARTIAL|NOT_COMPLETED")
                String status,
        @Size(max = 300) String note) {}
