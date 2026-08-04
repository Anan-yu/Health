package com.rayk.health.security.dto;

import jakarta.validation.constraints.NotBlank;

/** Verifies the pre-configured platform administrator before binding the current WeChat identity. */
public record WeChatAdminLoginRequest(
        @NotBlank String code, @NotBlank String username, @NotBlank String password) {}
