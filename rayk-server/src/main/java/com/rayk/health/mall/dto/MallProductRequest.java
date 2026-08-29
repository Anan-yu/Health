package com.rayk.health.mall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MallProductRequest(
        @NotBlank @Size(max = 120) String productName,
        @Size(max = 255) String subtitle,
        String description,
        @Size(max = 500) String mainImageUrl,
        @NotNull @Min(1) Integer priceCent,
        @NotNull @Min(0) Integer stock,
        @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status,
        @NotNull @Min(0) Integer sortOrder) {}
