package com.rayk.health.indicator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIndicatorRequest(
        @NotBlank @Size(max = 80) String indicatorCode,
        @NotBlank @Size(max = 100) String indicatorName,
        @NotBlank @Size(max = 30) String standardUnit,
        @NotBlank @Size(max = 50) String categoryCode) {}
