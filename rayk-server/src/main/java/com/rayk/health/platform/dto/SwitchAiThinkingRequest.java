package com.rayk.health.platform.dto;

import jakarta.validation.constraints.NotNull;

public record SwitchAiThinkingRequest(@NotNull Boolean enabled) {}
