package com.rayk.health.patient.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UpdateProfileRequestValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsImplausibleHeightAndWeight() {
        UpdateProfileRequest request = request(new BigDecimal("20"), new BigDecimal("900"));

        assertThat(validator.validate(request)).hasSize(2);
    }

    @Test
    void acceptsValidOptionalProfile() {
        UpdateProfileRequest request = request(new BigDecimal("168.5"), new BigDecimal("62.5"));

        assertThat(validator.validate(request)).isEmpty();
    }

    private UpdateProfileRequest request(BigDecimal height, BigDecimal weight) {
        return new UpdateProfileRequest(
                height,
                weight,
                new BigDecimal("78"),
                new BigDecimal("-1.5"),
                "A",
                "balanced",
                "none",
                "none",
                "none",
                "none",
                "NEVER",
                "NONE",
                "3_5_PER_WEEK",
                "GOOD",
                new BigDecimal("7.5"),
                "MEDIUM",
                "GOOD",
                "LOW",
                "balanced",
                "均衡饮食，外卖较少",
                "NO",
                "NO",
                "NO",
                "NO");
    }
}
