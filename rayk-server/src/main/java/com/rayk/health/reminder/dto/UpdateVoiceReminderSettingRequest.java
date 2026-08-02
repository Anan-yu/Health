package com.rayk.health.reminder.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record UpdateVoiceReminderSettingRequest(
        @NotNull Boolean mealEnabled,
        @NotNull LocalTime mealTime,
        @NotNull Boolean sleepEnabled,
        @NotNull LocalTime sleepTime) {}
