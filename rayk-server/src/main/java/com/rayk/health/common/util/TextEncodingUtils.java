package com.rayk.health.common.util;

import java.nio.charset.StandardCharsets;

/** Utilities for recovering text that was accidentally encoded as UTF-8 twice. */
public final class TextEncodingUtils {
    private static final String MOJIBAKE_MARKERS = "ÃÂâåæçèéêëìíîïñòóôõöùúûüýþÿ";

    private TextEncodingUtils() {}

    /**
     * Converts common UTF-8-as-Latin-1 mojibake back to Unicode when the conversion is unambiguous.
     * Normal Chinese, English, and valid Latin text are returned unchanged.
     */
    public static String repairUtf8Mojibake(String value) {
        if (value == null || value.isBlank()) return value;

        String current = value;
        for (int attempt = 0; attempt < 2 && hasMojibakeMarker(current); attempt++) {
            byte[] latin1Bytes = current.getBytes(StandardCharsets.ISO_8859_1);
            String candidate = new String(latin1Bytes, StandardCharsets.UTF_8);
            if (candidate.indexOf('\uFFFD') >= 0 || !isBetter(candidate, current)) break;
            current = candidate;
        }
        return current;
    }

    private static boolean hasMojibakeMarker(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (MOJIBAKE_MARKERS.indexOf(value.charAt(index)) >= 0) return true;
        }
        return false;
    }

    private static boolean isBetter(String candidate, String original) {
        return countCjk(candidate) > countCjk(original)
                || countMojibakeMarkers(candidate) < countMojibakeMarkers(original);
    }

    private static int countCjk(String value) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if ((character >= '\u3400' && character <= '\u4DBF')
                    || (character >= '\u4E00' && character <= '\u9FFF')) {
                count++;
            }
        }
        return count;
    }

    private static int countMojibakeMarkers(String value) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (MOJIBAKE_MARKERS.indexOf(value.charAt(index)) >= 0) count++;
        }
        return count;
    }
}
