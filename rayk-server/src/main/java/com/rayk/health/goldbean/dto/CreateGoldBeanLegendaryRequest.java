package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGoldBeanLegendaryRequest(
        @NotBlank @Size(max = 32) String phone,
        @Size(max = 255) String note) {}
