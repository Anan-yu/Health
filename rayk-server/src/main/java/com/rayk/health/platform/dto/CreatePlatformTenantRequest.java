package com.rayk.health.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePlatformTenantRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$") String tenantCode,
        @NotBlank @Size(max = 100) String tenantName,
        @NotBlank @Size(max = 50) String servicePlan,
        @NotBlank @Size(max = 50) String adminName,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String adminPhone) {}
