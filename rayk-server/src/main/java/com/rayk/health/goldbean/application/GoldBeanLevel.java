package com.rayk.health.goldbean.application;

import java.util.Arrays;

/** Direct-referral thresholds defined by the new ordinary-member specification. */
public enum GoldBeanLevel {
    ORDINARY("ORDINARY", "普通会员", 0, 0),
    COPPER("COPPER", "铜牌会员", 2, 100),
    SILVER("SILVER", "银牌会员", 9, 300),
    GOLD("GOLD", "金牌会员", 29, 600),
    DIAMOND("DIAMOND", "钻石会员", 50, 1000);

    private final String code;
    private final String displayName;
    private final int referralThreshold;
    private final int unlockReward;

    GoldBeanLevel(String code, String displayName, int referralThreshold, int unlockReward) {
        this.code = code;
        this.displayName = displayName;
        this.referralThreshold = referralThreshold;
        this.unlockReward = unlockReward;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public int referralThreshold() {
        return referralThreshold;
    }

    public int unlockReward() {
        return unlockReward;
    }

    public GoldBeanLevel next() {
        return this == DIAMOND ? DIAMOND : values()[ordinal() + 1];
    }

    public static GoldBeanLevel fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(ORDINARY);
    }

    public static GoldBeanLevel forDirectReferralCount(int count) {
        GoldBeanLevel result = ORDINARY;
        for (GoldBeanLevel level : values()) {
            if (count >= level.referralThreshold) result = level;
        }
        return result;
    }
}
