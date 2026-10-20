package com.bankpro.controller;

import com.bankpro.dto.ReportResult;
import com.bankpro.report.CsvWriter;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** /api/reports/{type} where type = customer-acquisition | lead-pipeline | conversion | product-sales |
 *  officer-performance | target-achievement | follow-up-completion. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserProvider currentUser;

    public ReportController(ReportService reportService, CurrentUserProvider currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    @GetMapping("/{type}")
    public ReportResult run(@PathVariable String type,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to,
                            @RequestParam(required = false) Long branchId,
                            @RequestParam(required = false) Long officerId,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        return reportService.run(currentUser.get(), ReportService.Type.parse(type), from, to, branchId, officerId, page, size);
    }

    /** Same report as CSV (all rows, scoped, formula-injection safe). */
    @GetMapping("/{type}/export")
    public void export(@PathVariable String type,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to,
                       @RequestParam(required = false) Long branchId,
                       @RequestParam(required = false) Long officerId,
                       HttpServletRequest http, HttpServletResponse response) throws IOException {
        ReportService.Type t = ReportService.Type.parse(type);
        ReportResult r = reportService.export(currentUser.get(), t, from, to, branchId, officerId, http.getRemoteAddr());
        String filename = "bankpro-" + t.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-" + r.from() + "-to-" + r.to() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        CsvWriter csv = new CsvWriter(writer).writeBom();
        csv.row(r.columns().stream().map(ReportResult.Column::label).toList());
        for (var row : r.rows()) {
            csv.row(r.columns().stream().map(c -> row.get(c.key())).toList());
        }
        csv.row(List.of());
        csv.row(List.of("Period", r.from() + " to " + r.to() + " (Bangladesh time)"));
        csv.row(List.of("Note", "Portfolio demonstration with fictional data. Sales are simulated."));
        writer.flush();
    }
}
