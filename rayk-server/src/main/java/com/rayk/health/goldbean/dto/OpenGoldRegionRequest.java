package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpenGoldRegionRequest(
        @NotBlank(message = "城市不能为空") @Size(max = 64, message = "城市名称长度不能超过64位") String city,
        /**
         * Retained for old clients only. The server derives the parent from the direct referral
         * chain and rejects a caller-provided value that does not match it.
         */
        @Size(max = 30, message = "上级区域标识无效") String parentRegionId) {}
