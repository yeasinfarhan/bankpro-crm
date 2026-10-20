package com.bankpro.report;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Minimal RFC-4180 CSV writer with protection against spreadsheet formula injection.
 * <p>
 * If a cell starts with = + - @ (or tab / carriage return), Excel may execute it as a formula when the
 * file is opened — e.g. a customer named {@code =HYPERLINK("http://evil",...)}. Such cells are prefixed
 * with a single quote so they are always shown as plain text (OWASP "CSV Injection" guidance).
 */
public final class CsvWriter {

    private final Writer out;

    public CsvWriter(Writer out) {
        this.out = out;
    }

    /** UTF-8 byte-order mark so Excel opens Bangla/UTF-8 text correctly. */
    public CsvWriter writeBom() throws IOException {
        out.write('﻿');
        return this;
    }

    public CsvWriter row(List<?> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                out.write(',');
            }
            out.write(escape(cells.get(i)));
        }
        out.write("\r\n");
        return this;
    }

    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = neutraliseFormula(value.toString());
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (needsQuotes) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** Plain signed numbers such as phone "+8801712345678" or "-50.25" are safe and left untouched. */
    private static final java.util.regex.Pattern PLAIN_NUMBER = java.util.regex.Pattern.compile("^[+-]?\\d[\\d.]*$");

    static String neutraliseFormula(String s) {
        if (PLAIN_NUMBER.matcher(s).matches()) {
            return s;
        }
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            return "'" + s;
        }
        return s;
    }
}
