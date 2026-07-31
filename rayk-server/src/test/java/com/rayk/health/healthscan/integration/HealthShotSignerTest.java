package com.rayk.health.healthscan.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthShotSignerTest {
    @Test
    void signsParametersInAsciiOrderAndExcludesVideoAndSign() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("timestamp", 1720000000000L);
        parameters.put("outUserId", "customer-1");
        parameters.put("appId", "demo-app");
        parameters.put("video", "ignored");
        parameters.put("sign", "ignored");

        String actual = HealthShotSigner.sign(parameters, "demo-key");

        assertThat(actual)
                .isEqualTo(
                        HealthShotSigner.md5(
                                "appId=demo-app&outUserId=customer-1&timestamp=1720000000000&key=demo-key"
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}

