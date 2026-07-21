package com.rayk.health.system.vo;

import java.time.LocalDateTime;

public record PlatformSupportTicketVo(
        String id,
        String tenantId,
        String submitterUserId,
        String category,
        String content,
        String contact,
        String status,
        String reply,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
