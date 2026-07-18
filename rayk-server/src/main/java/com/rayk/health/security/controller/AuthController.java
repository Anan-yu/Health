package com.rayk.health.security.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.security.dto.AuthData;
import com.rayk.health.security.dto.MockLoginRequest;
import com.rayk.health.security.dto.ProfileData;
import com.rayk.health.security.service.AuthService;
import com.rayk.health.security.service.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/mock-login")
    public ApiResponse<AuthData> mockLogin(@Valid @RequestBody MockLoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(null);
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileData> profile() {
        return ApiResponse.success(authService.profile());
    }

    @GetMapping("/permissions")
    public ApiResponse<List<String>> permissions() {
        return ApiResponse.success(CurrentUser.require().permissions());
    }
}

