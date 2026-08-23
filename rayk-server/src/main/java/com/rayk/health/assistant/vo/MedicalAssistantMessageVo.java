package com.rayk.health.assistant.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MedicalAssistantMessageVo(
        String id,
        String role,
        String content,
        String riskLevel,
        boolean emergency,
        String recommendedAction,
        List<String> citations,
        List<String> usedContext,
        List<String> followupQuestions,
        String model,
        LocalDateTime createdAt) {
    public MedicalAssistantMessageVo {
        citations = citations == null ? List.of() : citations;
        usedContext = usedContext == null ? List.of() : usedContext;
        followupQuestions = followupQuestions == null ? List.of() : followupQuestions;
    }
}
