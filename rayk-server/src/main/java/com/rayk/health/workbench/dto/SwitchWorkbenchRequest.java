package com.rayk.health.workbench.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchWorkbenchRequest(@NotBlank String code) {}

