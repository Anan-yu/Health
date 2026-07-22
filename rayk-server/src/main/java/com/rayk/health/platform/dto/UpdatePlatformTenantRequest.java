package com.rayk.health.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePlatformTenantRequest(
        @NotBlank @Size(max = 100) String tenantName,
        @NotBlank @Size(max = 50) String servicePlan,
        @NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status) {}
