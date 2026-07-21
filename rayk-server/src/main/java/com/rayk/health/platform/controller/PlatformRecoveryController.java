package com.rayk.health.platform.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.platform.application.HealthReportArtifactRecoveryService;
import com.rayk.health.platform.dto.HealthReportArtifactRecoveryData;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/recovery")
public class PlatformRecoveryController {
    private final HealthReportArtifactRecoveryService recoveryService;

    public PlatformRecoveryController(HealthReportArtifactRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/health-report-artifacts")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<HealthReportArtifactRecoveryData> recoverHealthReportArtifacts() {
        return ApiResponse.success(recoveryService.recoverMissingArtifacts());
    }
}
