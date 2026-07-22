package com.rayk.health.security.dto;

import jakarta.validation.constraints.NotBlank;

/** phoneCode is returned by WeChat getPhoneNumber and is required for automatic account matching. */
public record WeChatLoginRequest(@NotBlank String code, String phoneCode) {}

