package com.bankpro.validation;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Normalises contact details BEFORE validation and duplicate checks, so "01712-345678", "8801712345678"
 * and "+880 1712 345678" are recognised as the same number.
 */
public final class ContactNormalizer {

    /** Bangladesh mobile: 01[3-9] followed by 8 digits. */
    private static final Pattern BD_MOBILE_LOCAL = Pattern.compile("^01[3-9]\\d{8}$");

    private ContactNormalizer() {
    }

    /** Returns +8801XXXXXXXXX, or empty if the input is not a valid Bangladesh mobile number. */
    public static Optional<String> normalizePhone(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("[\\s\\-().]", "");
        if (digits.startsWith("+880")) {
            digits = digits.substring(3);          // keep the leading 0 -> 01XXXXXXXXX
        } else if (digits.startsWith("880")) {
            digits = digits.substring(2);
        }
        if (!BD_MOBILE_LOCAL.matcher(digits).matches()) {
            return Optional.empty();
        }
        return Optional.of("+88" + digits);
    }

    /** Trims and lower-cases; returns null for blank input (email is optional for customers). */
    public static String normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /** Collapses internal whitespace and trims. Null-safe; blank becomes null. */
    public static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }
}
