package com.rayk.health.laboratory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ConfirmIndicatorsRequest(@NotEmpty List<@Valid IndicatorInput> indicators) {}

