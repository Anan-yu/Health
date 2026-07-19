package com.rayk.health.security.wechat;

public record WeChatSessionIdentity(String appId, String openid, String unionid) {}

