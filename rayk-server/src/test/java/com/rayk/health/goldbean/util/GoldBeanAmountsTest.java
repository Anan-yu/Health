package com.rayk.health.goldbean.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GoldBeanAmountsTest {
    @Test
    void normalizesEveryBeanAmountToSixDecimalPlaces() {
        BigDecimal normalized = GoldBeanAmounts.normalize(new BigDecimal("1.23456789"));

        assertThat(normalized).isEqualByComparingTo(new BigDecimal("1.234567"));
        assertThat(normalized.scale()).isEqualTo(GoldBeanAmounts.SCALE);
    }

    @Test
    void splitsDecimalRewardsWithoutLosingTheBean() {
        BigDecimal amount = new BigDecimal("11.123456");
        BigDecimal trading = GoldBeanAmounts.tradingHalf(amount);
        BigDecimal bank = GoldBeanAmounts.bankHalf(amount);

        assertThat(trading).isEqualByComparingTo(new BigDecimal("5.561728"));
        assertThat(bank).isEqualByComparingTo(new BigDecimal("5.561728"));
        assertThat(bank.add(trading)).isEqualByComparingTo(amount);
    }

    @Test
    void truncatesPercentageOnlyAfterCalculatingTheExactDecimalReward() {
        assertThat(GoldBeanAmounts.percentage(new BigDecimal("11.123456"), 20))
                .isEqualByComparingTo(new BigDecimal("2.224691"));
    }

    @Test
    void roundsFractionalBeanPaymentUpToTheNextCent() {
        assertThat(GoldBeanAmounts.currencyCents(new BigDecimal("1.234567"), 100))
                .isEqualTo(124L);
    }
}
