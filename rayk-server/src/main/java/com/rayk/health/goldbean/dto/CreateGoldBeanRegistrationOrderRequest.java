package com.rayk.health.goldbean.dto;

import jakarta.validation.constraints.Size;

public record CreateGoldBeanRegistrationOrderRequest(
        @Size(max = 64) String referralCode,
        @Size(max = 64) String platformInviteCode,
        @Size(max = 64) String city) {
}
