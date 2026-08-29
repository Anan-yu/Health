package com.rayk.health.mall.vo;

public record MallAddressVo(
        String id,
        String receiverName,
        String receiverPhone,
        String province,
        String city,
        String district,
        String detailAddress,
        boolean isDefault) {}
