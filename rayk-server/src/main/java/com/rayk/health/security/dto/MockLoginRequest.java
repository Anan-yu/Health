package com.rayk.health.security.dto;

import jakarta.validation.constraints.NotBlank;

public record MockLoginRequest(@NotBlank String username, @NotBlank String password) {}

