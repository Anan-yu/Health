package com.rayk.health.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchAiModelRequest(@NotBlank String modelCode) {}
