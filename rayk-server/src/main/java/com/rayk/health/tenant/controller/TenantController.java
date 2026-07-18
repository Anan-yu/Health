package com.rayk.health.tenant.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.tenant.vo.StaffVo;
import com.rayk.health.tenant.vo.TenantProfileVo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant")
public class TenantController {
    @GetMapping("/profile")
    public ApiResponse<TenantProfileVo> profile() {
        return ApiResponse.success(
                new TenantProfileVo(
                        String.valueOf(CurrentUser.require().tenantId()),
                        "RayK测试健康中心",
                        "ACTIVE",
                        "DEVELOPMENT"));
    }

    @GetMapping("/staff")
    public ApiResponse<List<StaffVo>> staff() {
        return ApiResponse.success(
                List.of(
                        new StaffVo("10002", "机构管理员", List.of("TENANT_ADMIN"), "ACTIVE"),
                        new StaffVo("10003", "测试医生", List.of("DOCTOR"), "ACTIVE"),
                        new StaffVo("10004", "测试健康管理师", List.of("HEALTH_MANAGER"), "ACTIVE")));
    }
}

