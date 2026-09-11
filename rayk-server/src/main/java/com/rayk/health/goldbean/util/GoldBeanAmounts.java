package com.rayk.health.goldbean.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared fixed-point rules for all gold-bean quantities and balances. */
public final class GoldBeanAmounts {
    public static final int SCALE = 6;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);
    public static final BigDecimal TWO = BigDecimal.valueOf(2);
    public static final BigDecimal MINIMUM = BigDecimal.ONE.movePointLeft(SCALE);

    private GoldBeanAmounts() {}

    public static BigDecimal normalize(BigDecimal value) {
        return (value == null ? ZERO : value).setScale(SCALE, RoundingMode.DOWN);
    }

    public static BigDecimal nonNegative(BigDecimal value) {
        BigDecimal normalized = normalize(value);
        return normalized.signum() < 0 ? ZERO : normalized;
    }

    public static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static BigDecimal percentage(BigDecimal amount, int ratePercent) {
        if (!positive(amount) || ratePercent <= 0) return ZERO;
        if (ratePercent >= 100) return normalize(amount);
        return normalize(amount.multiply(BigDecimal.valueOf(ratePercent))
                .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.DOWN));
    }

    /** Splits a reward without losing any fraction; the trading bucket receives the floor half. */
    public static BigDecimal tradingHalf(BigDecimal amount) {
        BigDecimal normalized = normalize(amount);
        return normalize(normalized.divide(TWO, SCALE, RoundingMode.DOWN));
    }

    public static BigDecimal bankHalf(BigDecimal amount) {
        BigDecimal normalized = normalize(amount);
        return normalize(normalized.subtract(tradingHalf(normalized)));
    }

    /** Converts a bean quantity at a cent-denominated unit price to payable cents. */
    public static long currencyCents(BigDecimal quantity, long unitPriceCent) {
        if (!positive(quantity) || unitPriceCent <= 0) return 0L;
        return quantity.multiply(BigDecimal.valueOf(unitPriceCent))
                .setScale(0, RoundingMode.CEILING)
                .longValueExact();
    }
}
