package com.rayk.health.reminder.dto;

import jakarta.validation.constraints.Pattern;

public record VoiceReminderPreviewRequest(
        @Pattern(regexp = "MEAL|SLEEP", message = "提醒类型仅支持 MEAL 或 SLEEP") String type) {}
