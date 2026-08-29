package com.rayk.health.mall.vo;

public record MallProductVo(
        String id,
        String productName,
        String subtitle,
        String description,
        String mainImageUrl,
        int priceCent,
        int stock,
        int soldCount,
        String status,
        int sortOrder) {}
