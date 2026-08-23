package com.rayk.health.assistant.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MedicalAssistantConversationVo(
        String id,
        String title,
        String status,
        String model,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MedicalAssistantMessageVo> messages) {
    public MedicalAssistantConversationVo {
        messages = messages == null ? List.of() : messages;
    }
}
