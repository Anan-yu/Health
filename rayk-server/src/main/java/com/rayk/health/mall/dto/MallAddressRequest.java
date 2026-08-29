package com.rayk.health.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MallAddressRequest(
        @NotBlank @Size(max = 64) String receiverName,
        @NotBlank @Pattern(regexp = "1\\d{10}") String receiverPhone,
        @NotBlank @Size(max = 64) String province,
        @NotBlank @Size(max = 64) String city,
        @NotBlank @Size(max = 64) String district,
        @NotBlank @Size(max = 255) String detailAddress,
        boolean isDefault) {}
