package com.rayk.health.goldbean.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoldBeanPropertiesTest {
    @Test
    void calculatesBuyerAmountWithoutChangingBusinessAmount() {
        GoldBeanProperties properties = new GoldBeanProperties(
                true, true, 60, 60, 20, 100_000, 100_000, 7, 50, true,
                "normal_member_998", "test_member", "gold_bean", 100, 10_000,
                12, "机器人权益服务群", "");

        assertThat(properties.surchargedPaymentAmountCent(100)).isEqualTo(112);
        assertThat(properties.surchargedPaymentAmountCent(100_000)).isEqualTo(112_000);
        assertThat(properties.surchargedPaymentAmountCent(101)).isEqualTo(114);
    }

    @Test
    void zeroSurchargeKeepsTheConfiguredBusinessAmount() {
        GoldBeanProperties properties = new GoldBeanProperties(
                true, true, 60, 60, 20, 100_000, 100_000, 7, 50, true,
                "normal_member_998", "test_member", "gold_bean", 100, 10_000,
                0, "机器人权益服务群", "");

        assertThat(properties.surchargedPaymentAmountCent(100)).isEqualTo(100);
    }
}
