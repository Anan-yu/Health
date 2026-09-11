package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record CreateGoldBeanPurchaseOrderRequest(
        @DecimalMin(value = "0.000001")
        @DecimalMax(value = "10000")
        @Digits(integer = 5, fraction = 6)
        BigDecimal quantity) {
    public CreateGoldBeanPurchaseOrderRequest(long quantity) {
        this(BigDecimal.valueOf(quantity));
    }
}
