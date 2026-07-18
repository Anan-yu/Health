package com.rayk.health.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePatientRequest(
        @NotBlank @Size(max = 50) String name,
        @Pattern(regexp = "MALE|FEMALE|UNKNOWN") String gender,
        LocalDate birthDate,
        @Size(max = 30) String phone,
        Long assignedDoctorId,
        Long assignedManagerId) {}

