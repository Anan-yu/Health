package com.rayk.health.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A doctor is pre-registered by the platform and is matched on verified WeChat phone login. */
public record CreatePlatformDoctorRequest(
        @NotBlank @Size(max = 50) String displayName,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone) {}
