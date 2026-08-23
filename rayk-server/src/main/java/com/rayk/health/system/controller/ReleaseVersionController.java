package com.rayk.health.system.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.system.vo.ReleaseVersionVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class ReleaseVersionController {
    @GetMapping("/version")
    public ApiResponse<ReleaseVersionVo> version() {
        return ApiResponse.success(
                new ReleaseVersionVo(
                        environment("RAYK_RELEASE_ID", "dev-local"),
                        environment("RAYK_GIT_COMMIT", "unknown"),
                        Boolean.parseBoolean(environment("RAYK_GIT_DIRTY", "false")),
                        environment("RAYK_BUILD_TIME", "unknown"),
                        environment("RAYK_DATABASE_MIGRATION", "unknown"),
                        environment("RAYK_FRONTEND_H5_SHA256", "unknown"),
                        environment("RAYK_FRONTEND_MP_WEIXIN_DEV_SHA256", "unknown"),
                        environment("RAYK_FRONTEND_MP_WEIXIN_PROD_LAN_SHA256", "unknown")));
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
