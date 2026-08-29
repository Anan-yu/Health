package com.rayk.health.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MallOrderRequest(
        @NotBlank String productId,
        @NotBlank String addressId,
        @Min(1) @Max(99) int quantity) {}
