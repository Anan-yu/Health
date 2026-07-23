package com.rayk.health.platform.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.platform.application.PlatformOverviewService;
import com.rayk.health.platform.dto.UpdatePlatformTenantRequest;
import com.rayk.health.platform.dto.CreatePlatformTenantRequest;
import com.rayk.health.platform.dto.CreatePlatformDoctorRequest;
import com.rayk.health.platform.vo.PlatformOverviewVo;
import com.rayk.health.tenant.vo.TenantProfileVo;
import com.rayk.health.tenant.vo.StaffVo;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformController {
    private final PlatformOverviewService overviewService;

    public PlatformController(PlatformOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PlatformOverviewVo> overview() {
        return ApiResponse.success(overviewService.overview());
    }

    @PostMapping("/tenants")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<TenantProfileVo> createTenant(@Valid @RequestBody CreatePlatformTenantRequest request) {
        return ApiResponse.success(overviewService.createTenant(request));
    }

    @GetMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<TenantProfileVo> tenantProfile(@PathVariable long tenantId) {
        return ApiResponse.success(overviewService.tenantProfile(tenantId));
    }

    @PutMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<TenantProfileVo> updateTenant(
            @PathVariable long tenantId, @Valid @RequestBody UpdatePlatformTenantRequest request) {
        return ApiResponse.success(overviewService.updateTenant(tenantId, request));
    }

    @GetMapping("/tenants/{tenantId}/doctors")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<List<StaffVo>> doctors(@PathVariable long tenantId) {
        return ApiResponse.success(overviewService.doctors(tenantId));
    }

    @PostMapping("/tenants/{tenantId}/doctors")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<StaffVo> createDoctor(
            @PathVariable long tenantId, @Valid @RequestBody CreatePlatformDoctorRequest request) {
        return ApiResponse.success(overviewService.createDoctor(tenantId, request));
    }
}
