package com.bankpro.service;

import com.bankpro.audit.AuditService;
import com.bankpro.dto.ReportResult;
import com.bankpro.dto.ReportResult.Chart;
import com.bankpro.dto.ReportResult.Column;
import com.bankpro.dto.ReportResult.Metric;
import com.bankpro.dto.TargetResponse;
import com.bankpro.exception.NotFoundException;
import com.bankpro.metrics.MetricScope;
import com.bankpro.metrics.MetricsRepository;
import com.bankpro.metrics.Period;
import com.bankpro.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The seven performance reports. Scope (officer / branch / all) is applied BEFORE aggregation, so an export
 * can never contain rows the caller may not see. All aggregation happens in SQL (MetricsRepository).
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    public enum Type {
        CUSTOMER_ACQUISITION, LEAD_PIPELINE, CONVERSION, PRODUCT_SALES, OFFICER_PERFORMANCE, TARGET_ACHIEVEMENT,
        FOLLOW_UP_COMPLETION;

        public static Type parse(String slug) {
            try {
                return valueOf(slug.toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException e) {
                throw new NotFoundException("Unknown report: " + slug);
            }
        }
    }

    public static final int MAX_EXPORT_ROWS = 5000;

    private final MetricsRepository metrics;
    private final DashboardService dashboard;
    private final TargetService targetService;
    private final AuditService audit;
    private final Clock clock;

    public ReportService(MetricsRepository metrics, DashboardService dashboard, TargetService targetService,
                         AuditService audit, Clock clock) {
        this.metrics = metrics;
        this.dashboard = dashboard;
        this.targetService = targetService;
        this.audit = audit;
        this.clock = clock;
    }

    /** Runs a report and returns one page of its rows. */
    public ReportResult run(AuthenticatedUser caller, Type type, LocalDate from, LocalDate to, Long branchId,
                            Long officerId, int page, int size) {
        Full full = build(caller, type, dashboard.period(from, to), MetricScope.of(caller, branchId, officerId));
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int fromIdx = Math.min(safePage * safeSize, full.rows.size());
        int toIdx = Math.min(fromIdx + safeSize, full.rows.size());
        int totalPages = (int) Math.ceil(full.rows.size() / (double) safeSize);
        return new ReportResult(type.name(), full.title, full.description, full.period.from(), full.period.to(),
                full.columns, full.summary, full.rows.subList(fromIdx, toIdx), safePage, safeSize, full.rows.size(),
                totalPages, full.chart);
    }

    /** All rows (capped) for CSV export; the export is audit-logged without any row data. */
    @Transactional
    public ReportResult export(AuthenticatedUser caller, Type type, LocalDate from, LocalDate to, Long branchId,
                               Long officerId, String ip) {
        ReportResult all = run(caller, type, from, to, branchId, officerId, 0, Integer.MAX_VALUE);
        List<Map<String, Object>> rows = all.rows().size() > MAX_EXPORT_ROWS ? all.rows().subList(0, MAX_EXPORT_ROWS) : all.rows();
        audit.record(caller.id(), "REPORT_EXPORTED", "REPORT", null,
                Map.of("type", type.name(), "rows", rows.size(), "from", all.from().toString(), "to", all.to().toString()), ip);
        return new ReportResult(all.type(), all.title(), all.description(), all.from(), all.to(), all.columns(),
                all.summary(), rows, 0, rows.size(), rows.size(), 1, all.chart());
    }

    // ---------------------------------------------------------------- report definitions

    private record Full(String title, String description, Period period, List<Column> columns, List<Metric> summary,
                        List<Map<String, Object>> rows, Chart chart) {
    }

    private Full build(AuthenticatedUser caller, Type type, Period p, MetricScope scope) {
        return switch (type) {
            case CUSTOMER_ACQUISITION -> acquisition(p, scope);
            case LEAD_PIPELINE -> pipeline(p, scope);
            case CONVERSION -> conversion(p, scope);
            case PRODUCT_SALES -> productSales(p, scope);
            case OFFICER_PERFORMANCE -> officerPerformance(p, scope);
            case TARGET_ACHIEVEMENT -> targetAchievement(caller, p, scope);
            case FOLLOW_UP_COMPLETION -> followUpCompletion(p, scope);
        };
    }

    private Full acquisition(Period p, MetricScope scope) {
        var rows = new ArrayList<Map<String, Object>>();
        Map<String, Map<String, Object>> byMonth = new LinkedHashMap<>();
        metrics.customersByMonth(scope, p).forEach(r -> byMonth.put((String) r.get("month"), r));
        long total = 0;
        for (String month : p.months()) {
            Map<String, Object> src = byMonth.get(month);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            for (String k : List.of("newCustomers", "MASS", "EMERGING_AFFLUENT", "AFFLUENT", "SME")) {
                row.put(k, src == null || src.get(k) == null ? 0L : ((Number) src.get(k)).longValue());
            }
            total += (Long) row.get("newCustomers");
            rows.add(row);
        }
        return new Full("Customer acquisition", "New customers per month (Bangladesh time), by segment.", p,
                List.of(col("month", "Month", "month"), col("newCustomers", "New customers", "number"),
                        col("MASS", "Mass", "number"), col("EMERGING_AFFLUENT", "Emerging affluent", "number"),
                        col("AFFLUENT", "Affluent", "number"), col("SME", "SME", "number")),
                List.of(metric("New customers", total, "number"),
                        metric("Average per month", BigDecimal.valueOf(total).divide(BigDecimal.valueOf(rows.size()), 1, RoundingMode.HALF_UP), "number")),
                rows, new Chart("bar", "month", List.of(col("newCustomers", "New customers", "number"))));
    }

    private Full pipeline(Period p, MetricScope scope) {
        Map<String, Map<String, Object>> byStage = new LinkedHashMap<>();
        metrics.leadsByStage(scope, p).forEach(r -> byStage.put((String) r.get("stage"), r));
        var rows = new ArrayList<Map<String, Object>>();
        long open = 0;
        BigDecimal openValue = BigDecimal.ZERO;
        for (String stage : List.of("NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "CONVERTED", "LOST")) {
            Map<String, Object> src = byStage.get(stage);
            long leads = src == null ? 0 : ((Number) src.get("leads")).longValue();
            BigDecimal value = src == null ? BigDecimal.ZERO : (BigDecimal) src.get("estimatedValue");
            rows.add(row("stage", stage, "leads", leads, "estimatedValue", value));
            if (LeadStageRules.OPEN.stream().anyMatch(s -> s.name().equals(stage))) {
                open += leads;
                openValue = openValue.add(value);
            }
        }
        return new Full("Lead pipeline", "Leads created in the period, by their current stage. Values are estimates, not sales.", p,
                List.of(col("stage", "Stage", "stage"), col("leads", "Leads", "number"), col("estimatedValue", "Estimated value", "bdt")),
                List.of(metric("Open leads", open, "number"), metric("Open pipeline value (estimated)", openValue, "bdt")),
                rows, new Chart("bar", "stage", List.of(col("leads", "Leads", "number"))));
    }

    private Full conversion(Period p, MetricScope scope) {
        var rows = new ArrayList<Map<String, Object>>();
        long converted = 0;
        long lost = 0;
        for (Map<String, Object> r : metrics.officerActivity(scope, p, now())) {
            long c = n(r, "leadsConverted");
            long l = n(r, "leadsLost");
            converted += c;
            lost += l;
            rows.add(row("officer", r.get("officer"), "branch", r.get("branch"), "leadsCreated", n(r, "leadsCreated"),
                    "converted", c, "lost", l, "conversionRate", LeadService.conversionRate(c, l)));
        }
        return new Full("Conversion", "Leads created in the period and their outcome. Rate = converted ÷ (converted + lost).", p,
                List.of(col("officer", "Officer", "text"), col("branch", "Branch", "text"), col("leadsCreated", "Leads created", "number"),
                        col("converted", "Converted", "number"), col("lost", "Lost", "number"), col("conversionRate", "Conversion rate", "percent")),
                List.of(metric("Converted", converted, "number"), metric("Lost", lost, "number"),
                        metric("Conversion rate", LeadService.conversionRate(converted, lost), "percent")),
                rows, new Chart("bar", "officer", List.of(col("converted", "Converted", "number"), col("lost", "Lost", "number"))));
    }

    private Full productSales(Period p, MetricScope scope) {
        var rows = new ArrayList<Map<String, Object>>();
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;
        long pending = 0;
        for (Map<String, Object> r : metrics.salesByProduct(scope, p)) {
            BigDecimal value = (BigDecimal) r.get("completedValue");
            total = total.add(value);
            count += n(r, "completedCount");
            pending += n(r, "pendingCount");
            rows.add(row("product", r.get("product"), "category", r.get("category"), "completedCount", n(r, "completedCount"),
                    "completedValue", value, "pendingCount", n(r, "pendingCount"), "declinedCount", n(r, "declinedCount")));
        }
        return new Full("Product-wise sales", "Completed simulated sales by product. Pending loan/card applications are listed separately and not counted as sales.", p,
                List.of(col("product", "Product", "text"), col("category", "Category", "category"), col("completedCount", "Completed", "number"),
                        col("completedValue", "Completed value", "bdt"), col("pendingCount", "Pending approval", "number"),
                        col("declinedCount", "Declined", "number")),
                List.of(metric("Completed sales", count, "number"), metric("Completed value", total, "bdt"),
                        metric("Pending applications", pending, "number")),
                rows, new Chart("bar", "product", List.of(col("completedValue", "Completed value", "bdt"))));
    }

    private Full officerPerformance(Period p, MetricScope scope) {
        var rows = new ArrayList<Map<String, Object>>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> r : metrics.officerActivity(scope, p, now())) {
            total = total.add((BigDecimal) r.get("completedSalesValue"));
            rows.add(row("officer", r.get("officer"), "branch", r.get("branch"), "customersAdded", n(r, "customersAdded"),
                    "leadsCreated", n(r, "leadsCreated"), "conversions", n(r, "conversions"),
                    "completedSalesValue", r.get("completedSalesValue"), "followUpsCompleted", n(r, "followUpsCompleted"),
                    "overdueNow", n(r, "overdueNow")));
        }
        return new Full("Officer performance", "Activity and results per officer in the period. Overdue is the current count.", p,
                List.of(col("officer", "Officer", "text"), col("branch", "Branch", "text"), col("customersAdded", "Customers added", "number"),
                        col("leadsCreated", "Leads", "number"), col("conversions", "Conversions", "number"),
                        col("completedSalesValue", "Completed sales", "bdt"), col("followUpsCompleted", "Follow-ups done", "number"),
                        col("overdueNow", "Overdue now", "number")),
                List.of(metric("Officers", rows.size(), "number"), metric("Completed sales", total, "bdt")),
                rows, new Chart("bar", "officer", List.of(col("completedSalesValue", "Completed sales", "bdt"))));
    }

    private Full targetAchievement(AuthenticatedUser caller, Period p, MetricScope scope) {
        var rows = new ArrayList<Map<String, Object>>();
        int met = 0;
        List<TargetResponse> targets = targetService.list(caller, p.from(), p.to(), scope.branchId(), scope.officerId());
        for (TargetResponse t : targets) {
            if (t.achievementPct() != null && t.achievementPct().compareTo(BigDecimal.valueOf(100)) >= 0) met++;
            rows.add(row("owner", t.user() == null ? "Branch: " + t.branch().name() : t.user().fullName(),
                    "category", t.category().name(), "period", t.periodStart() + " – " + t.periodEnd(),
                    "unit", t.unit(), "targetValue", t.targetValue(), "actual", t.actual(),
                    "achievementPct", t.achievementPct(), "remaining", t.remaining()));
        }
        return new Full("Target achievement", "Targets overlapping the period with live actuals. Achievement = actual ÷ target × 100.", p,
                List.of(col("owner", "Target for", "text"), col("category", "Category", "category"), col("period", "Period", "text"),
                        col("targetValue", "Target", "unitValue"), col("actual", "Actual", "unitValue"),
                        col("achievementPct", "Achievement", "percent"), col("remaining", "Remaining", "unitValue")),
                List.of(metric("Targets", targets.size(), "number"), metric("Targets met", met, "number")),
                rows, new Chart("bar", "owner", List.of(col("achievementPct", "Achievement %", "percent"))));
    }

    private Full followUpCompletion(Period p, MetricScope scope) {
        var rows = new ArrayList<Map<String, Object>>();
        long scheduled = 0;
        long completed = 0;
        for (Map<String, Object> r : metrics.officerActivity(scope, p, now())) {
            long s = n(r, "followUpsScheduled");
            long c = n(r, "followUpsCompleted");
            scheduled += s;
            completed += c;
            rows.add(row("officer", r.get("officer"), "branch", r.get("branch"), "scheduled", s, "completed", c,
                    "completionRate", rate(c, s), "overdueNow", n(r, "overdueNow")));
        }
        return new Full("Follow-up completion", "Follow-ups due in the period and how many were completed.", p,
                List.of(col("officer", "Officer", "text"), col("branch", "Branch", "text"), col("scheduled", "Due in period", "number"),
                        col("completed", "Completed", "number"), col("completionRate", "Completion rate", "percent"),
                        col("overdueNow", "Overdue now", "number")),
                List.of(metric("Due in period", scheduled, "number"), metric("Completed", completed, "number"),
                        metric("Completion rate", rate(completed, scheduled), "percent")),
                rows, new Chart("bar", "officer", List.of(col("scheduled", "Due", "number"), col("completed", "Completed", "number"))));
    }

    // ---------------------------------------------------------------- helpers

    private java.time.LocalDateTime now() {
        return MetricsRepository.utc(clock.instant());
    }

    static BigDecimal rate(long part, long whole) {
        return whole == 0 ? null : BigDecimal.valueOf(part * 100L).divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }

    private static long n(Map<String, Object> r, String key) {
        Object v = r.get(key);
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static Column col(String key, String label, String format) {
        return new Column(key, label, format);
    }

    private static Metric metric(String label, Object value, String format) {
        return new Metric(label, value, format);
    }

    private static Map<String, Object> row(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
