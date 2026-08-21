package com.rayk.health.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Replaces the platform administrator's verified-phone login identity. */
public record UpdatePlatformAdminPhoneRequest(
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone) {}
