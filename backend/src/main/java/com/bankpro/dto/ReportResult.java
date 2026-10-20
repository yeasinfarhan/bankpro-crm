package com.bankpro.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Generic report payload so one frontend page can render every report.
 *
 * @param columns column definitions; {@code format} = text | number | bdt | percent | month
 * @param rows    the requested page of rows
 * @param chart   which columns to plot (x axis + series)
 */
public record ReportResult(
        String type,
        String title,
        String description,
        LocalDate from,
        LocalDate to,
        List<Column> columns,
        List<Metric> summary,
        List<Map<String, Object>> rows,
        int page,
        int size,
        long totalElements,
        int totalPages,
        Chart chart
) {
    public record Column(String key, String label, String format) {
    }

    public record Metric(String label, Object value, String format) {
    }

    public record Chart(String type, String xKey, List<Column> series) {
    }
}
