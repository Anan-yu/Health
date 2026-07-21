package com.rayk.health.platform.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.platform.application.PlatformOverviewService;
import com.rayk.health.platform.vo.PlatformOverviewVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
