package com.rayk.health.indicator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateModelRequest(
        @NotBlank @Size(max = 80) String modelCode,
        @NotBlank @Size(max = 100) String modelName,
        @NotBlank @Size(max = 50) String modelCategory,
        @NotBlank @Size(max = 30) String version,
        String configSnapshot) {}
