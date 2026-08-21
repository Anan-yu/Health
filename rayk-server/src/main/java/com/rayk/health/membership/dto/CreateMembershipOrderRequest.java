package com.rayk.health.membership.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMembershipOrderRequest(@NotBlank String planCode) {}
