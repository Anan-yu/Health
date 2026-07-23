package com.rayk.health.patient.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UpdatePatientIdentityRequestValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsSupportedGender() {
        assertThat(
                        validator.validate(
                                new UpdatePatientIdentityRequest(
                                        "测试客户",
                                        "FEMALE",
                                        LocalDate.of(1988, 5, 20),
                                        "13800138000")))
                .isEmpty();
    }

    @Test
    void rejectsUnknownGender() {
        assertThat(
                        validator.validate(
                                new UpdatePatientIdentityRequest(
                                        "测试客户",
                                        "UNKNOWN",
                                        LocalDate.of(1988, 5, 20),
                                        "13800138000")))
                .anyMatch(violation -> "gender".equals(violation.getPropertyPath().toString()));
    }
}
