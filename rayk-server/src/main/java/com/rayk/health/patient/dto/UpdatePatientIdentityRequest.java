package com.rayk.health.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Customer-maintained identity fields used for hospital report lookup. */
public record UpdatePatientIdentityRequest(
        @NotBlank @Size(max = 50) String name,
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone) {}
