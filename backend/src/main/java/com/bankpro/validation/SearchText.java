package com.bankpro.validation;

import java.util.Locale;

/** Helpers for user-typed search text used in SQL LIKE (always passed as a bound parameter). */
public final class SearchText {

    private SearchText() {
    }

    /** "%term%" in lower case with LIKE wildcards escaped, or null when the term is blank. */
    public static String containsPattern(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String escaped = term.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
