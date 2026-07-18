package com.rayk.health.laboratory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record IndicatorInput(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull BigDecimal value,
        @NotBlank String unit,
        BigDecimal referenceLow,
        BigDecimal referenceHigh) {}

