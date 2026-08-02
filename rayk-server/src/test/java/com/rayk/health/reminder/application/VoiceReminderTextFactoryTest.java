package com.rayk.health.reminder.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VoiceReminderTextFactoryTest {
    private final VoiceReminderTextFactory factory = new VoiceReminderTextFactory();

    @Test
    void createsWarmMealCopyWithPatientName() {
        for (int index = 0; index < 24; index++) {
            assertThat(factory.create("MEAL", "王阿姨"))
                    .startsWith("王阿姨")
                    .containsAnyOf("吃饭", "饭点", "饭菜")
                    .doesNotContain("恋人", "对象", "唯一", "只需要我");
        }
    }

    @Test
    void createsSleepCopyAndFallsBackToFriendlyAddress() {
        assertThat(factory.create("SLEEP", " "))
                .startsWith("朋友")
                .containsAnyOf("睡觉", "睡眠", "休息", "夜深", "夜晚");
    }
}
