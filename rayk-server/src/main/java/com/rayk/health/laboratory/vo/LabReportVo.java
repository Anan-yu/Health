package com.rayk.health.laboratory.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LabReportVo(
        String id,
        String patientId,
        String reportName,
        LocalDate reportDate,
        String status,
        String sourceType,
        List<IndicatorVo> indicators,
        LocalDateTime createdAt) {}

