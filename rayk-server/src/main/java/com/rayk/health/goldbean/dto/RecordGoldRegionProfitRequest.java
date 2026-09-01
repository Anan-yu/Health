package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Platform settlement input; amount is expressed in whole gold beans, never cash. */
public record RecordGoldRegionProfitRequest(
        @NotNull(message = "区域不能为空") Long regionId,
        @Min(value = 1, message = "分润金额必须大于0") long amount,
        @NotBlank(message = "幂等键不能为空") @Size(max = 120, message = "幂等键过长") String idempotencyKey) {}
