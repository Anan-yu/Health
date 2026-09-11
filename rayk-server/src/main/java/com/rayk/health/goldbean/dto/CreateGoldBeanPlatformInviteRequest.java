package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateGoldBeanPlatformInviteRequest(
        @Size(max = 20, message = "绑定手机号长度不能超过20位") String boundPhone,
        @Min(value = 1, message = "授权码有效期至少为1小时")
        @Max(value = 168, message = "授权码有效期不能超过168小时")
                Integer validHours) {}
