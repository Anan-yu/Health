package com.rayk.health.security.wechat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** Keeps mobile numbers out of application tables while retaining deterministic matching. */
public final class PhoneIdentity {
    private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

    private PhoneIdentity() {}

    public static String normalize(String phone) {
        String normalized = phone == null ? "" : phone.replaceAll("[\\s-]", "");
        if (!MOBILE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid mobile number");
        }
        return normalized;
    }

    public static String hash(String phone) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(normalize(phone).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static String mask(String phone) {
        String normalized = normalize(phone);
        return normalized.substring(0, 3) + "****" + normalized.substring(7);
    }
}
