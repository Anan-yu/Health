package com.rayk.health.patient.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rayk.health.patient.entity.HealthProfileEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProfileCompletenessCalculatorTest {

    @Test
    void countsOnlyActuallyAnsweredQuestionnaireFields() {
        HealthProfileEntity profile = new HealthProfileEntity();
        profile.setHeightCm(BigDecimal.valueOf(168.5));
        profile.setWeightKg(BigDecimal.valueOf(62.5));
        profile.setMedicalHistory("none");
        profile.setSmokingStatus("NEVER");

        assertThat(ProfileCompletenessCalculator.calculate(profile)).isEqualTo(18);
    }

    @Test
    void ignoresNullEmptyAndWhitespaceOnlyFields() {
        HealthProfileEntity profile = new HealthProfileEntity();
        profile.setBloodType("");
        profile.setMedicalHistory("   ");
        profile.setLifestyleSummary(null);

        assertThat(ProfileCompletenessCalculator.calculate(profile)).isZero();
    }
}
