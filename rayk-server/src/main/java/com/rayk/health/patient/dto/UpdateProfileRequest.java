package com.rayk.health.patient.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProfileRequest(
        @DecimalMin("50") @DecimalMax("250") BigDecimal heightCm,
        @DecimalMin("10") @DecimalMax("500") BigDecimal weightKg,
        @Size(max = 20) String bloodType,
        @Size(max = 2000) String lifestyleSummary,
        @Size(max = 2000) String medicalHistory,
        @Size(max = 2000) String allergyHistory,
        @Size(max = 2000) String currentMedications,
        @Size(max = 30) String smokingStatus,
        @Size(max = 30) String alcoholStatus,
        @Size(max = 30) String exerciseFrequency,
        @Size(max = 30) String sleepQuality,
        @Size(max = 30) String stressLevel,
        @Size(max = 200) String dietaryPreference) {}
