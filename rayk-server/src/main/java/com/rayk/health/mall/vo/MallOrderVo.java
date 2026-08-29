package com.rayk.health.mall.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MallOrderVo(
        String orderNo,
        String status,
        int amountCent,
        String paymentChannel,
        String transactionId,
        String receiverName,
        String receiverPhone,
        String province,
        String city,
        String district,
        String detailAddress,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        List<MallOrderItemVo> items) {}
