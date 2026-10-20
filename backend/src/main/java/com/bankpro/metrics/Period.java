package com.bankpro.metrics;

import com.bankpro.exception.InvalidFieldException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * An inclusive range of Bangladesh calendar dates, converted to the UTC bounds the database stores.
 * SQL always uses {@code col >= :start AND col < :end} (end = the day after {@code to}, at midnight BST).
 */
public record Period(LocalDate from, LocalDate to, ZoneId zone) {

    public static final int MAX_DAYS = 3 * 366;

    public Period {
        if (from.isAfter(to)) {
            throw new InvalidFieldException("from", "The start date must be on or before the end date.");
        }
        if (from.plusDays(MAX_DAYS).isBefore(to)) {
            throw new InvalidFieldException("to", "Choose a range of at most three years.");
        }
    }

    /** Default for dashboards: the last 6 months including the current one. */
    public static Period resolve(LocalDate from, LocalDate to, LocalDate today, ZoneId zone) {
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : YearMonth.from(end).minusMonths(5).atDay(1);
        return new Period(start, end, zone);
    }

    public Instant startInstant() {
        return from.atStartOfDay(zone).toInstant();
    }

    public Instant endInstant() {
        return to.plusDays(1).atStartOfDay(zone).toInstant();
    }

    /** DATETIME columns hold UTC wall-clock values, so bind UTC LocalDateTimes. */
    public LocalDateTime startUtc() {
        return LocalDateTime.ofInstant(startInstant(), ZoneOffset.UTC);
    }

    public LocalDateTime endUtc() {
        return LocalDateTime.ofInstant(endInstant(), ZoneOffset.UTC);
    }

    /** Every month touched by the range, e.g. ["2026-05", ..., "2026-10"], so charts show zero months too. */
    public List<String> months() {
        List<String> months = new ArrayList<>();
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
            months.add(m.toString());
        }
        return months;
    }
}
