package com.rayk.health.security.dto;

/** A short-lived, single-use code used to bind a staff account to a personal-subject mini program. */
public record WeChatStaffInviteData(String code, long expiresIn) {}
