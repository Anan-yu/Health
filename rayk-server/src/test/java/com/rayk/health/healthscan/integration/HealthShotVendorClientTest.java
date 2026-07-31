package com.rayk.health.healthscan.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.healthscan.config.HealthShotProperties;
import org.junit.jupiter.api.Test;

class HealthShotVendorClientTest {
    private final HealthShotVendorClient client =
            new HealthShotVendorClient(
                    new HealthShotProperties(
                            true,
                            "app",
                            "key",
                            "http://127.0.0.1",
                            "/upload",
                            "UAT",
                            "provider",
                            "latest",
                            null,
                            5,
                            10),
                    new ObjectMapper());

    @Test
    void parsesNestedCamelCaseVitals() {
        HealthShotVendorResult result =
                client.parse(
                        """
                        {
                          "code": 0,
                          "data": {
                            "detectId": "detect-1",
                            "heartRate": 72,
                            "heartRateVariability": 48.5,
                            "oxygenSaturation": 98,
                            "respirationRate": 16,
                            "systolicBloodPressure": 118,
                            "diastolicBloodPressure": 76,
                            "stressHrv": 31
                          }
                        }
                        """);

        assertThat(result.completed()).isTrue();
        assertThat(result.failed()).isFalse();
        assertThat(result.detectId()).isEqualTo("detect-1");
        assertThat(result.heartRate()).isEqualByComparingTo("72");
        assertThat(result.systolicBloodPressure()).isEqualByComparingTo("118");
    }

    @Test
    void keepsUnknownSuccessfulResponseProcessingInsteadOfInventingVitals() {
        HealthShotVendorResult result =
                client.parse("{\"code\":0,\"data\":{\"taskId\":\"pending-1\"}}");

        assertThat(result.completed()).isFalse();
        assertThat(result.failed()).isFalse();
        assertThat(result.detectId()).isEqualTo("pending-1");
    }
}
