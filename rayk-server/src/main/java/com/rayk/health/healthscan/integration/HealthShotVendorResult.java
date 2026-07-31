package com.rayk.health.healthscan.integration;

import java.math.BigDecimal;

public record HealthShotVendorResult(
        String code,
        String message,
        String detectId,
        BigDecimal heartRate,
        BigDecimal heartRateVariability,
        BigDecimal oxygenSaturation,
        BigDecimal respirationRate,
        BigDecimal systolicBloodPressure,
        BigDecimal diastolicBloodPressure,
        BigDecimal stressHrv,
        BigDecimal qualityScore,
        boolean completed,
        boolean failed,
        String rawJson) {}

