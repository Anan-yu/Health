package com.rayk.health.patient.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HealthProfileVo(
        String id,
        String patientId,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bmi,
        String bloodType,
        String lifestyleSummary,
        String medicalHistory,
        String allergyHistory,
        String currentMedications,
        String smokingStatus,
        String alcoholStatus,
        String exerciseFrequency,
        String sleepQuality,
        String stressLevel,
        String dietaryPreference,
        int profileCompleteness,
        LocalDateTime updatedAt) {}
