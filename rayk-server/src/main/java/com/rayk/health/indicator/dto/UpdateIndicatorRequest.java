package com.rayk.health.indicator.dto;

import jakarta.validation.constraints.Size;

public record UpdateIndicatorRequest(
        @Size(max = 100) String indicatorName,
        @Size(max = 30) String standardUnit,
        @Size(max = 50) String categoryCode,
        Integer enabled) {}
