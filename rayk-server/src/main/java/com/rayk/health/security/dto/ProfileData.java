package com.rayk.health.security.dto;

import com.rayk.health.security.service.WorkbenchOption;
import java.util.List;

public record ProfileData(
        String userId,
        String tenantId,
        String tenantName,
        String username,
        String displayName,
        List<String> roles,
        List<String> permissions,
        List<WorkbenchOption> availableWorkbenches,
        String currentWorkbench) {}

