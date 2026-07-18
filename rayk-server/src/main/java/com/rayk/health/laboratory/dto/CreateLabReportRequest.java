package com.rayk.health.laboratory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateLabReportRequest(
        @NotNull Long patientId,
        @NotBlank @Size(max = 100) String reportName,
        LocalDate reportDate,
        @Size(max = 30) String sourceType) {}

