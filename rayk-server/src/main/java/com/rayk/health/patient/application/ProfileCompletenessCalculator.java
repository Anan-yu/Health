package com.rayk.health.patient.application;

import com.rayk.health.patient.entity.HealthProfileEntity;

/**
 * 档案完整度计算器：统计非空字段数 / 总字段数 * 100。
 */
public final class ProfileCompletenessCalculator {

    private ProfileCompletenessCalculator() {}

    private static final int TOTAL_FIELDS = 13;

    public static int calculate(HealthProfileEntity entity) {
        int nonNull = 0;
        if (entity.getHeightCm() != null) nonNull++;
        if (entity.getWeightKg() != null) nonNull++;
        if (hasText(entity.getBloodType())) nonNull++;
        if (hasText(entity.getLifestyleSummary())) nonNull++;
        if (hasText(entity.getMedicalHistory())) nonNull++;
        if (hasText(entity.getAllergyHistory())) nonNull++;
        if (hasText(entity.getCurrentMedications())) nonNull++;
        if (hasText(entity.getSmokingStatus())) nonNull++;
        if (hasText(entity.getAlcoholStatus())) nonNull++;
        if (hasText(entity.getExerciseFrequency())) nonNull++;
        if (hasText(entity.getSleepQuality())) nonNull++;
        if (hasText(entity.getStressLevel())) nonNull++;
        if (hasText(entity.getDietaryPreference())) nonNull++;
        return (int) Math.round((double) nonNull / TOTAL_FIELDS * 100);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
