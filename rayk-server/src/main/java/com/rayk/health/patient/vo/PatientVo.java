package com.rayk.health.patient.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientVo(
        String id,
        String name,
        String gender,
        LocalDate birthDate,
        String phoneMasked,
        String status,
        String assignedDoctorId,
        String assignedManagerId,
        LocalDateTime createdAt) {}

