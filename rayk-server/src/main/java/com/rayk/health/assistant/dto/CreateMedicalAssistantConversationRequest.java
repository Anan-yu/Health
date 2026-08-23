package com.rayk.health.assistant.dto;

import jakarta.validation.constraints.Size;

public record CreateMedicalAssistantConversationRequest(@Size(max = 80) String title) {}
