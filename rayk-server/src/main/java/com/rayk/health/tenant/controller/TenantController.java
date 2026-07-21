package com.rayk.health.tenant.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.tenant.application.TenantAdminService;
import com.rayk.health.tenant.vo.StaffVo;
import com.rayk.health.tenant.vo.TenantProfileVo;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant")
public class TenantController {
    private final TenantAdminService tenantAdminService;

    public TenantController(TenantAdminService tenantAdminService) {
        this.tenantAdminService = tenantAdminService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('tenant:staff:manage')")
    public ApiResponse<TenantProfileVo> profile() {
        return ApiResponse.success(tenantAdminService.profile());
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAuthority('tenant:staff:manage')")
    public ApiResponse<List<StaffVo>> staff() {
        return ApiResponse.success(tenantAdminService.staff());
    }
}
