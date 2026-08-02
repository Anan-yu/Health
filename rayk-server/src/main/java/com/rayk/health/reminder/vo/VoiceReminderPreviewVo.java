package com.rayk.health.reminder.vo;

public record VoiceReminderPreviewVo(
        long id,
        String type,
        String text,
        String voiceName,
        String audioUrl) {}
