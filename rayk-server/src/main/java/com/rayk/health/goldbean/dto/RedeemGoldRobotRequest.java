package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedeemGoldRobotRequest(
        @NotBlank(message = "兑换请求号不能为空") @Size(max = 80, message = "兑换请求号过长")
                String clientRequestId) {}
