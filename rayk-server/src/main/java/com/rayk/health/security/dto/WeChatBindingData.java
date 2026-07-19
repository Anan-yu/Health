package com.rayk.health.security.dto;

import java.time.LocalDateTime;

public record WeChatBindingData(
        String userId, String appId, String openidMasked, String status, LocalDateTime boundAt) {}

