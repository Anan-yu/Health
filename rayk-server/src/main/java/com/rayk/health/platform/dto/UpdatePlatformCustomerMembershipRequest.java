package com.rayk.health.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Platform-admin action for enabling or disabling a customer's annual membership. */
public record UpdatePlatformCustomerMembershipRequest(
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
        @NotNull Boolean active) {}
