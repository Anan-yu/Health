package com.rayk.health.goldbean.vo;

import java.util.List;

public record GoldBeanReferralBranchVo(
        String displayName,
        String memberLevelName,
        int directReferralCount,
        int hiddenMemberCount,
        List<GoldBeanReferralMemberVo> members) {}
