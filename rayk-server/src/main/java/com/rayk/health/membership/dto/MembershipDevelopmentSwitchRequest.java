package com.rayk.health.membership.dto;

import jakarta.validation.constraints.NotBlank;

/** Development-only membership state switch. It is rejected unless the server is in development mode. */
public record MembershipDevelopmentSwitchRequest(@NotBlank String target) {}
