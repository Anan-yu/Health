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
        if (entity.getBloodType() != null) nonNull++;
        if (entity.getLifestyleSummary() != null) nonNull++;
        if (entity.getMedicalHistory() != null) nonNull++;
        if (entity.getAllergyHistory() != null) nonNull++;
        if (entity.getCurrentMedications() != null) nonNull++;
        if (entity.getSmokingStatus() != null) nonNull++;
        if (entity.getAlcoholStatus() != null) nonNull++;
        if (entity.getExerciseFrequency() != null) nonNull++;
        if (entity.getSleepQuality() != null) nonNull++;
        if (entity.getStressLevel() != null) nonNull++;
        if (entity.getDietaryPreference() != null) nonNull++;
        return (int) Math.round((double) nonNull / TOTAL_FIELDS * 100);
    }
}
