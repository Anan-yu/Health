package com.rayk.health.healthscan.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HealthScanResultVo(
        String id,
        String status,
        String statusLabel,
        String vendorDetectId,
        BigDecimal heartRate,
        BigDecimal heartRateVariability,
        BigDecimal oxygenSaturation,
        BigDecimal respirationRate,
        BigDecimal systolicBloodPressure,
        BigDecimal diastolicBloodPressure,
        BigDecimal stressHrv,
        BigDecimal qualityScore,
        String failureMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {}

