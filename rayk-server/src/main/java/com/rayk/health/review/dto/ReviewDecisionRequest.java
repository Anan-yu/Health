package com.rayk.health.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(@NotBlank @Size(max = 1000) String opinion) {}

