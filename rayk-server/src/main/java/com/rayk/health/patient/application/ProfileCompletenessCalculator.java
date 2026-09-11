package com.rayk.health.patient.application;

import com.rayk.health.patient.entity.HealthProfileEntity;
import com.rayk.health.patient.entity.PatientEntity;

/**
 * 档案完整度计算器：统计非空字段数 / 总字段数 * 100。
 */
public final class ProfileCompletenessCalculator {

    private ProfileCompletenessCalculator() {}

    private static final int QUESTIONNAIRE_FIELDS = 24;
    private static final int IDENTITY_FIELDS = 3;

    /**
     * 保留健康问卷单独计算的兼容入口；页面展示请使用带患者身份的重载方法。
     */
    public static int calculate(HealthProfileEntity entity) {
        return percentage(countQuestionnaireFields(entity), QUESTIONNAIRE_FIELDS);
    }

    /**
     * 计算页面展示用完整度：姓名、性别、出生日期 + 24 项健康问卷，共 27 项。
     */
    public static int calculate(HealthProfileEntity entity, PatientEntity patient) {
        int nonNull = countQuestionnaireFields(entity);
        if (patient != null) {
            if (hasText(patient.getName())) nonNull++;
            if (hasText(patient.getGender())) nonNull++;
            if (patient.getBirthDate() != null) nonNull++;
        }
        return percentage(nonNull, QUESTIONNAIRE_FIELDS + IDENTITY_FIELDS);
    }

    private static int countQuestionnaireFields(HealthProfileEntity entity) {
        int nonNull = 0;
        if (entity.getHeightCm() != null) nonNull++;
        if (entity.getWeightKg() != null) nonNull++;
        if (entity.getWaistCm() != null) nonNull++;
        if (entity.getRecentWeightChangeKg() != null) nonNull++;
        if (hasText(entity.getBloodType())) nonNull++;
        if (hasText(entity.getLifestyleSummary())) nonNull++;
        if (hasText(entity.getMedicalHistory())) nonNull++;
        if (hasText(entity.getFamilyHistory())) nonNull++;
        if (hasText(entity.getAllergyHistory())) nonNull++;
        if (hasText(entity.getCurrentMedications())) nonNull++;
        if (hasText(entity.getSmokingStatus())) nonNull++;
        if (hasText(entity.getAlcoholStatus())) nonNull++;
        if (hasText(entity.getExerciseFrequency())) nonNull++;
        if (hasText(entity.getSleepQuality())) nonNull++;
        if (entity.getSleepHours() != null) nonNull++;
        if (hasText(entity.getStressLevel())) nonNull++;
        if (hasText(entity.getMoodStatus())) nonNull++;
        if (hasText(entity.getFearLevel())) nonNull++;
        if (hasText(entity.getDietaryPreference())) nonNull++;
        if (hasText(entity.getRecentDietaryPattern())) nonNull++;
        if (hasText(entity.getDiabetesStatus())) nonNull++;
        if (hasText(entity.getHypertensionStatus())) nonNull++;
        if (hasText(entity.getDyslipidemiaStatus())) nonNull++;
        if (hasText(entity.getFattyLiverStatus())) nonNull++;
        return nonNull;
    }

    private static int percentage(int nonNull, int totalFields) {
        return (int) Math.round((double) nonNull / totalFields * 100);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
