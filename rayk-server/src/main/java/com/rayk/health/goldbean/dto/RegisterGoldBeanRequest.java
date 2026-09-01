package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.Size;

/** The dev pilot records the registration recipient; it never pretends to have charged money. */
public record RegisterGoldBeanRequest(
        @Size(max = 32, message = "推荐码长度不能超过32位") String referralCode,
        @Size(max = 64, message = "城市名称长度不能超过64位") String city) {}
