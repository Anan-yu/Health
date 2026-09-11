package com.rayk.health.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TextEncodingUtilsTest {
    @Test
    void repairsUtf8TextThatWasDecodedAsLatin1() {
        String original = "河南省 / 驻马店市";
        String mojibake = new String(original.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

        assertThat(TextEncodingUtils.repairUtf8Mojibake(mojibake)).isEqualTo(original);
    }

    @Test
    void leavesValidTextUnchanged() {
        assertThat(TextEncodingUtils.repairUtf8Mojibake("河南省 / 驻马店市")).isEqualTo("河南省 / 驻马店市");
        assertThat(TextEncodingUtils.repairUtf8Mojibake("Paris")).isEqualTo("Paris");
        assertThat(TextEncodingUtils.repairUtf8Mojibake("Café")).isEqualTo("Café");
    }
}
