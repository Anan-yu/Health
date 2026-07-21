package com.rayk.health.indicator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddAliasRequest(
        @NotBlank @Size(max = 100) String aliasName,
        @NotBlank @Size(max = 50) String source) {}
