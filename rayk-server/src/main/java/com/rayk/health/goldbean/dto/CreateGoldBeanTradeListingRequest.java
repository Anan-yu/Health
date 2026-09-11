package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.Locale;

public record CreateGoldBeanTradeListingRequest(
        @DecimalMin(value = "0.000001")
        @DecimalMax(value = "1000000")
        @Digits(integer = 7, fraction = 6)
        BigDecimal quantity,
        String bucket) {
    public CreateGoldBeanTradeListingRequest(BigDecimal quantity) {
        this(quantity, "TRADING");
    }

    public CreateGoldBeanTradeListingRequest(long quantity) {
        this(BigDecimal.valueOf(quantity), "TRADING");
    }

    public CreateGoldBeanTradeListingRequest(long quantity, String bucket) {
        this(BigDecimal.valueOf(quantity), bucket);
    }

    public String normalizedBucket() {
        return bucket == null || bucket.isBlank() ? "TRADING" : bucket.trim().toUpperCase(Locale.ROOT);
    }
}
