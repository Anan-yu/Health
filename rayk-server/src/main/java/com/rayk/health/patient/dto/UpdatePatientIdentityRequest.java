package com.rayk.health.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Customer-maintained identity fields used for hospital report lookup. */
public record UpdatePatientIdentityRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank
                @Pattern(
                        regexp = "MALE|FEMALE",
                        message = "性别必须选择男或女")
                String gender,
        @NotNull @PastOrPresent(message = "出生日期不能晚于今天") LocalDate birthDate,
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone) {}
