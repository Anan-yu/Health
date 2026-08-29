package com.rayk.health.mall.vo;

public record MallOrderItemVo(
        String productId,
        String productName,
        String mainImageUrl,
        int unitPriceCent,
        int quantity,
        int totalCent) {}
