package com.rayk.health.security.dto;

import jakarta.validation.constraints.NotBlank;

public record WeChatLoginRequest(@NotBlank String code) {}

