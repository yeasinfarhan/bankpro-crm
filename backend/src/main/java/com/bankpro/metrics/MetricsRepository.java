package com.bankpro.metrics;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * All dashboard / report / target aggregations, written as plain SQL so the logic is easy to read and verify.
 * <ul>
 *   <li>Every value is a bound parameter (":name") — never string-concatenated user input.</li>
 *   <li>Scope conditions are added from {@link MetricScope}, so an officer can only ever aggregate their own rows.</li>
 *   <li>Months are bucketed in Bangladesh time: CONVERT_TZ(utc_column, '+00:00', '+06:00').</li>
 *   <li>A sale counts as completed on {@value #COMPLETED_AT} — approval date for loans/cards, conversion date otherwise.</li>
 * </ul>
 */
@Repository
public class MetricsRepository {

    /** When a COMPLETED sale counts: approved loans/cards on the decision date, others when converted. */
    public static final String COMPLETED_AT = "COALESCE(s.decided_at, s.converted_at)";
    private static final String BST = "'+06:00'";

    private final NamedParameterJdbcTemplate jdbc;

    public MetricsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ helpers

    private static MapSqlParameterSource params(MetricScope scope, Period period) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("branchId", scope.branchId());
        p.addValue("officerId", scope.officerId());
        if (period != null) {
            p.addValue("start", period.startUtc());
            p.addValue("end", period.endUtc());
        }
        return p;
    }

    /** " AND alias.branch_id = :branchId AND alias.<officerColumn> = :officerId" for the parts that apply. */
    private static String scope(MetricScope scope, String alias, String officerColumn) {
        StringBuilder sql = new StringBuilder();
        if (scope.branchId() != null) sql.append(" AND ").append(alias).append(".branch_id = :branchId");
        if (scope.officerId() != null && officerColumn != null) {
            sql.append(" AND ").append(alias).append('.').append(officerColumn).append(" = :officerId");
        }
        return sql.toString();
    }

    private static String month(String column) {
        return "DATE_FORMAT(CONVERT_TZ(" + column + ", '+00:00', " + BST + "), '%Y-%m')";
    }

    private long count(String sql, MapSqlParameterSource p) {
        Long n = jdbc.queryForObject(sql, p, Long.class);
        return n == null ? 0 : n;
    }

    private BigDecimal sum(String sql, MapSqlParameterSource p) {
        BigDecimal v = jdbc.queryForObject(sql, p, BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    // ------------------------------------------------------------------ KPIs

    /** Non-archived customers (officer scope = customers assigned to the officer). */
    public long totalCustomers(MetricScope scope) {
        return count("SELECT COUNT(*) FROM customers c WHERE c.status <> 'ARCHIVED'"
                + scope(scope, "c", "assigned_officer_id"), params(scope, null));
    }

    /** Customers created in the period (officer scope = customers the officer added). */
    public long newCustomers(MetricScope scope, Period period) {
        return count("SELECT COUNT(*) FROM customers c WHERE c.created_at >= :start AND c.created_at < :end"
                + scope(scope, "c", "created_by"), params(scope, period));
    }

    public long openLeads(MetricScope scope) {
        return count("SELECT COUNT(*) FROM leads l WHERE l.status IN ('NEW','CONTACTED','QUALIFIED','PROPOSAL')"
                + scope(scope, "l", "assigned_officer_id"), params(scope, null));
    }

    /** Leads converted in the period = sales rows (one per lead) except declined applications. */
    public long convertedLeads(MetricScope scope, Period period) {
        return count("SELECT COUNT(*) FROM sales s WHERE s.status <> 'DECLINED'"
                + " AND s.converted_at >= :start AND s.converted_at < :end"
                + scope(scope, "s", "officer_id"), params(scope, period));
    }

    /** Value of COMPLETED sales in the period; {@code category} null = all products. */
    public BigDecimal completedSalesValue(MetricScope scope, Period period, String category) {
        String sql = "SELECT COALESCE(SUM(s.amount), 0) FROM sales s JOIN banking_products p ON p.id = s.product_id"
                + " WHERE s.status = 'COMPLETED' AND " + COMPLETED_AT + " >= :start AND " + COMPLETED_AT + " < :end"
                + (category == null ? "" : " AND p.category = :category")
                + scope(scope, "s", "officer_id");
        MapSqlParameterSource p = params(scope, period).addValue("category", category);
        return sum(sql, p);
    }

    public long completedSalesCount(MetricScope scope, Period period, String category) {
        String sql = "SELECT COUNT(*) FROM sales s JOIN banking_products p ON p.id = s.product_id"
                + " WHERE s.status = 'COMPLETED' AND " + COMPLETED_AT + " >= :start AND " + COMPLETED_AT + " < :end"
                + (category == null ? "" : " AND p.category = :category")
                + scope(scope, "s", "officer_id");
        return count(sql, params(scope, period).addValue("category", category));
    }

    /** LOAN leads created in the period (a lead count — NOT loan sales). */
    public long loanLeads(MetricScope scope, Period period) {
        return count("SELECT COUNT(*) FROM leads l JOIN banking_products p ON p.id = l.product_id"
                + " WHERE p.category = 'LOAN' AND l.created_at >= :start AND l.created_at < :end"
                + scope(scope, "l", "assigned_officer_id"), params(scope, period));
    }

    public long overdueFollowUps(MetricScope scope, LocalDateTime nowUtc) {
        return count("SELECT COUNT(*) FROM follow_up_activities f WHERE f.status = 'SCHEDULED' AND f.scheduled_at < :now"
                + scope(scope, "f", "assigned_officer_id"), params(scope, null).addValue("now", nowUtc));
    }

    // ------------------------------------------------------------------ chart series

    /** month → {completedValue, completedCount, depositValue} for COMPLETED sales. */
    public List<Map<String, Object>> salesByMonth(MetricScope scope, Period period) {
        String m = month(COMPLETED_AT);
        return jdbc.queryForList("SELECT " + m + " AS month, SUM(s.amount) AS completedValue, COUNT(*) AS completedCount,"
                + " SUM(CASE WHEN p.category = 'DEPOSIT' THEN s.amount ELSE 0 END) AS depositValue"
                + " FROM sales s JOIN banking_products p ON p.id = s.product_id"
                + " WHERE s.status = 'COMPLETED' AND " + COMPLETED_AT + " >= :start AND " + COMPLETED_AT + " < :end"
                + scope(scope, "s", "officer_id") + " GROUP BY month ORDER BY month", params(scope, period));
    }

    /** month → {newCustomers, MASS, EMERGING_AFFLUENT, AFFLUENT, SME}. */
    public List<Map<String, Object>> customersByMonth(MetricScope scope, Period period) {
        return jdbc.queryForList("SELECT " + month("c.created_at") + " AS month, COUNT(*) AS newCustomers,"
                + " SUM(c.segment = 'MASS') AS MASS, SUM(c.segment = 'EMERGING_AFFLUENT') AS EMERGING_AFFLUENT,"
                + " SUM(c.segment = 'AFFLUENT') AS AFFLUENT, SUM(c.segment = 'SME') AS SME"
                + " FROM customers c WHERE c.created_at >= :start AND c.created_at < :end"
                + scope(scope, "c", "created_by") + " GROUP BY month ORDER BY month", params(scope, period));
    }

    /** Leads created in the period, grouped by their CURRENT stage, with estimated value. */
    public List<Map<String, Object>> leadsByStage(MetricScope scope, Period period) {
        return jdbc.queryForList("SELECT l.status AS stage, COUNT(*) AS leads, COALESCE(SUM(l.estimated_value), 0) AS estimatedValue"
                + " FROM leads l WHERE l.created_at >= :start AND l.created_at < :end"
                + scope(scope, "l", "assigned_officer_id") + " GROUP BY l.status", params(scope, period));
    }

    /** Per product: completed count/value (by completion date) and pending/declined counts (by conversion date). */
    public List<Map<String, Object>> salesByProduct(MetricScope scope, Period period) {
        return jdbc.queryForList("SELECT p.id AS productId, p.name AS product, p.category AS category,"
                + " SUM(s.status = 'COMPLETED' AND " + COMPLETED_AT + " >= :start AND " + COMPLETED_AT + " < :end) AS completedCount,"
                + " COALESCE(SUM(CASE WHEN s.status = 'COMPLETED' AND " + COMPLETED_AT + " >= :start AND " + COMPLETED_AT
                + " < :end THEN s.amount END), 0) AS completedValue,"
                + " SUM(s.status = 'PENDING_APPROVAL' AND s.converted_at >= :start AND s.converted_at < :end) AS pendingCount,"
                + " SUM(s.status = 'DECLINED' AND s.converted_at >= :start AND s.converted_at < :end) AS declinedCount"
                + " FROM sales s JOIN banking_products p ON p.id = s.product_id WHERE 1 = 1"
                + scope(scope, "s", "officer_id")
                + " GROUP BY p.id, p.name, p.category"
                + " HAVING completedCount + pendingCount + declinedCount > 0 ORDER BY completedValue DESC, product",
                params(scope, period));
    }

    /**
     * One row per active officer/manager in scope with their activity in the period.
     * Built from small aggregated sub-queries (no rows are loaded into Java memory).
     */
    public List<Map<String, Object>> officerActivity(MetricScope scope, Period period, LocalDateTime nowUtc) {
        String sql = """
                SELECT u.id AS officerId, u.full_name AS officer, b.name AS branch,
                  (SELECT COUNT(*) FROM customers c WHERE c.created_by = u.id AND c.created_at >= :start AND c.created_at < :end) AS customersAdded,
                  (SELECT COUNT(*) FROM leads l WHERE l.assigned_officer_id = u.id AND l.created_at >= :start AND l.created_at < :end) AS leadsCreated,
                  (SELECT COUNT(*) FROM leads l WHERE l.assigned_officer_id = u.id AND l.created_at >= :start AND l.created_at < :end AND l.status = 'CONVERTED') AS leadsConverted,
                  (SELECT COUNT(*) FROM leads l WHERE l.assigned_officer_id = u.id AND l.created_at >= :start AND l.created_at < :end AND l.status = 'LOST') AS leadsLost,
                  (SELECT COUNT(*) FROM leads l WHERE l.assigned_officer_id = u.id AND l.status IN ('NEW','CONTACTED','QUALIFIED','PROPOSAL')) AS openLeads,
                  (SELECT COUNT(*) FROM sales s WHERE s.officer_id = u.id AND s.status <> 'DECLINED' AND s.converted_at >= :start AND s.converted_at < :end) AS conversions,
                  (SELECT COALESCE(SUM(s.amount), 0) FROM sales s WHERE s.officer_id = u.id AND s.status = 'COMPLETED'
                     AND COALESCE(s.decided_at, s.converted_at) >= :start AND COALESCE(s.decided_at, s.converted_at) < :end) AS completedSalesValue,
                  (SELECT COUNT(*) FROM follow_up_activities f WHERE f.assigned_officer_id = u.id AND f.scheduled_at >= :start AND f.scheduled_at < :end) AS followUpsScheduled,
                  (SELECT COUNT(*) FROM follow_up_activities f WHERE f.assigned_officer_id = u.id AND f.scheduled_at >= :start AND f.scheduled_at < :end AND f.status = 'COMPLETED') AS followUpsCompleted,
                  (SELECT COUNT(*) FROM follow_up_activities f WHERE f.assigned_officer_id = u.id AND f.status = 'SCHEDULED' AND f.scheduled_at < :now) AS overdueNow
                FROM users u JOIN branches b ON b.id = u.branch_id
                WHERE u.active = TRUE AND u.role IN ('RELATIONSHIP_OFFICER', 'BRANCH_MANAGER')
                """ + (scope.branchId() != null ? " AND u.branch_id = :branchId" : "")
                + (scope.officerId() != null ? " AND u.id = :officerId" : "")
                + " ORDER BY completedSalesValue DESC, conversions DESC, officer";
        return jdbc.queryForList(sql, params(scope, period).addValue("now", nowUtc));
    }

    // ------------------------------------------------------------------ targets

    /**
     * Actual achievement for one target, using the definitions in docs/ARCHITECTURE.md §5.
     * {@code userId} null = branch-level target (whole branch).
     */
    public BigDecimal targetActual(String category, long branchId, Long userId, Period period) {
        MetricScope scope = new MetricScope(branchId, userId);
        return switch (category) {
            case "NEW_CUSTOMERS" -> BigDecimal.valueOf(newCustomers(scope, period));
            case "DEPOSIT_SALES" -> completedSalesValue(scope, period, "DEPOSIT");
            case "CREDIT_CARD_SALES" -> BigDecimal.valueOf(completedSalesCount(scope, period, "CARD"));
            case "LOAN_LEADS" -> BigDecimal.valueOf(loanLeads(scope, period));
            case "CONVERTED_LEADS" -> BigDecimal.valueOf(convertedLeads(scope, period));
            default -> throw new IllegalArgumentException("Unknown target category " + category);
        };
    }

    public static LocalDateTime utc(java.time.Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
