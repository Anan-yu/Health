package com.rayk.health.security.dto;

import jakarta.validation.constraints.NotBlank;

public record WeChatStaffLoginRequest(@NotBlank String code, @NotBlank String inviteCode) {}
