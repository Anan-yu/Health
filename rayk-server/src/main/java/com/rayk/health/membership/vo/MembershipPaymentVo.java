package com.rayk.health.membership.vo;

/** 微信小程序 wx.requestVirtualPayment 所需的调起参数。 */
public record MembershipPaymentVo(
        String mode,
        String signData,
        String paySig,
        String signature) {}
