package com.rayk.health.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMedicalAssistantMessageRequest(
        @NotBlank(message = "请输入想咨询的健康问题") @Size(max = 4000) String content) {}
