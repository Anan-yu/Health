package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record BuyGoldBeanTradeRequest(
        @DecimalMin(value = "0.000001")
        @DecimalMax(value = "1000000")
        @Digits(integer = 7, fraction = 6)
        BigDecimal quantity) {
    public BuyGoldBeanTradeRequest(long quantity) {
        this(BigDecimal.valueOf(quantity));
    }
}
