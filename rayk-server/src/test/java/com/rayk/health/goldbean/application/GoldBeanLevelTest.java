package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoldBeanLevelTest {
    @Test
    void resolvesOnlyDirectReferralThresholds() {
        assertThat(GoldBeanLevel.forDirectReferralCount(0)).isEqualTo(GoldBeanLevel.ORDINARY);
        assertThat(GoldBeanLevel.forDirectReferralCount(1)).isEqualTo(GoldBeanLevel.ORDINARY);
        assertThat(GoldBeanLevel.forDirectReferralCount(2)).isEqualTo(GoldBeanLevel.COPPER);
        assertThat(GoldBeanLevel.forDirectReferralCount(9)).isEqualTo(GoldBeanLevel.SILVER);
        assertThat(GoldBeanLevel.forDirectReferralCount(29)).isEqualTo(GoldBeanLevel.GOLD);
        assertThat(GoldBeanLevel.forDirectReferralCount(50)).isEqualTo(GoldBeanLevel.DIAMOND);
        assertThat(GoldBeanLevel.forDirectReferralCount(500)).isEqualTo(GoldBeanLevel.DIAMOND);
    }

    @Test
    void keepsDiamondAsHighestLevel() {
        assertThat(GoldBeanLevel.DIAMOND.next()).isEqualTo(GoldBeanLevel.DIAMOND);
        assertThat(GoldBeanLevel.fromCode("diamond")).isEqualTo(GoldBeanLevel.DIAMOND);
    }

    @Test
    void createsReferralCodeWhenUserPartIsShort() {
        assertThat(GoldBeanApplicationService.createReferralCode(1L))
                .startsWith("SY1")
                .hasSize(11);
    }

    @Test
    void keepsReferralCodeWithinDatabaseColumnLimit() {
        assertThat(GoldBeanApplicationService.createReferralCode(Long.MAX_VALUE))
                .hasSize(20);
    }
}
