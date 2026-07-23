package com.rayk.health.patient.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HealthProfileVo(
        String id,
        String patientId,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal waistCm,
        BigDecimal recentWeightChangeKg,
        BigDecimal bmi,
        String bloodType,
        String lifestyleSummary,
        String medicalHistory,
        String familyHistory,
        String allergyHistory,
        String currentMedications,
        String smokingStatus,
        String alcoholStatus,
        String exerciseFrequency,
        String sleepQuality,
        BigDecimal sleepHours,
        String stressLevel,
        String moodStatus,
        String fearLevel,
        String dietaryPreference,
        String recentDietaryPattern,
        String diabetesStatus,
        String hypertensionStatus,
        String dyslipidemiaStatus,
        String fattyLiverStatus,
        int profileCompleteness,
        LocalDateTime updatedAt) {}
