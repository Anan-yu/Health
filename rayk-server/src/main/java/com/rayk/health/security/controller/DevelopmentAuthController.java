package com.rayk.health.security.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.security.dto.AuthData;
import com.rayk.health.security.dto.MockLoginRequest;
import com.rayk.health.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 本机调试专用。生产配置下该控制器不会创建，接口也不会存在。 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(
        prefix = "rayk.auth",
        name = "development-login-enabled",
        havingValue = "true")
public class DevelopmentAuthController {
    private final AuthService authService;

    public DevelopmentAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/mock-login")
    public ApiResponse<AuthData> mockLogin(@Valid @RequestBody MockLoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
