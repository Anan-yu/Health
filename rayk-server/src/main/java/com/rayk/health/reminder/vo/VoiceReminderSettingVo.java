package com.rayk.health.reminder.vo;

import java.time.LocalTime;

public record VoiceReminderSettingVo(
        boolean mealEnabled,
        LocalTime mealTime,
        boolean sleepEnabled,
        LocalTime sleepTime,
        String timezone,
        String voiceDescription,
        boolean serviceAvailable) {}
