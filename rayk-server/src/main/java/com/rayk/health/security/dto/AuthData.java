package com.rayk.health.security.dto;

import com.rayk.health.security.service.WorkbenchOption;
import java.util.List;

public record AuthData(
        String accessToken,
        String tokenType,
        long expiresIn,
        String userId,
        String tenantId,
        String tenantName,
        String displayName,
        List<String> roles,
        List<String> permissions,
        List<WorkbenchOption> availableWorkbenches,
        String defaultWorkbench) {}

