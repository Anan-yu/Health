package com.rayk.health.mall.vo;

/** Parameters returned by WeChat Pay API v3 for wx.requestPayment. */
public record MallPaymentVo(
        String timeStamp,
        String nonceStr,
        String packageValue,
        String signType,
        String paySign) {}
