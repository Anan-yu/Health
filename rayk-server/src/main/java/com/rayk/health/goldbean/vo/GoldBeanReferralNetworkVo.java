package com.rayk.health.goldbean.vo;

import java.util.List;

public record GoldBeanReferralNetworkVo(
        String selfDisplayName,
        String selfLevelName,
        int directReferralCount,
        int teamMemberCount,
        int hiddenDirectCount,
        List<GoldBeanReferralBranchVo> branches,
        List<GoldBeanReferralTrendPointVo> trend) {}
