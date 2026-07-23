package com.rayk.health.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Updates a pre-registered doctor. A blank phone keeps the current verified-phone match. */
public record UpdatePlatformDoctorRequest(
        @NotBlank @Size(max = 50) String displayName,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$") String phone) {}
