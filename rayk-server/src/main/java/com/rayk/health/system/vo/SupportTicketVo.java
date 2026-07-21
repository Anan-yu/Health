package com.rayk.health.system.vo;

import java.time.LocalDateTime;

public record SupportTicketVo(
    String id,
    String category,
    String content,
    String contact,
    String status,
    String reply,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
