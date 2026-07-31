package com.rayk.health.healthscan.integration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public final class HealthShotSigner {
    private HealthShotSigner() {}

    public static String sign(Map<String, ?> parameters, String key) {
        TreeMap<String, Object> sorted = new TreeMap<>();
        parameters.forEach(
                (name, value) -> {
                    if (value != null && !"sign".equals(name) && !"video".equals(name)) {
                        sorted.put(name, value);
                    }
                });
        StringBuilder content = new StringBuilder();
        sorted.forEach(
                (name, value) -> {
                    if (!content.isEmpty()) {
                        content.append('&');
                    }
                    content.append(name).append('=').append(value);
                });
        content.append("&key=").append(key);
        return md5(content.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String md5(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(content);
            StringBuilder value = new StringBuilder(32);
            for (byte item : digest) {
                value.append(String.format("%02x", item & 0xff));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 is unavailable", exception);
        }
    }
}

