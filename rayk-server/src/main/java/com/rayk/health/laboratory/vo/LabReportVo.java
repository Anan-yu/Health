package com.rayk.health.laboratory.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LabReportVo(
        String id,
        String patientId,
        String reportName,
        LocalDate reportDate,
        String status,
        Integer processingProgress,
        String processingMessage,
        String failureReason,
        String sourceType,
        List<IndicatorVo> indicators,
        List<OcrFindingVo> findings,
        boolean hasImageFiles,
        JsonNode imageAnalysis,
        LocalDateTime createdAt) {}

