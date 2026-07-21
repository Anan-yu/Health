package com.rayk.health.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.system.application.AuditService;
import com.rayk.health.system.application.AuditService.AuditQueryRequest;
import com.rayk.health.system.entity.OperationAuditLogEntity;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('platform:audit:list')")
    public ApiResponse<Page<OperationAuditLogEntity>> queryLogs(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        AuditQueryRequest request =
                new AuditQueryRequest(
                        tenantId, operationType, resourceType, startDate, endDate, page, size);
        return ApiResponse.success(auditService.queryLogs(request));
    }
}
