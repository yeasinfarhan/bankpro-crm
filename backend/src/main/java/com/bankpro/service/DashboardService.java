package com.bankpro.service;

import com.bankpro.config.AppProperties;
import com.bankpro.dto.DashboardSummary;
import com.bankpro.dto.TargetResponse;
import com.bankpro.entity.LeadStatus;
import com.bankpro.entity.Role;
import com.bankpro.exception.ForbiddenException;
import com.bankpro.metrics.MetricScope;
import com.bankpro.metrics.MetricsRepository;
import com.bankpro.metrics.Period;
import com.bankpro.repository.SalesTargetRepository;
import com.bankpro.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dashboard figures — every number is a SQL aggregation over real records in the caller's scope. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final MetricsRepository metrics;
    private final SalesTargetRepository targets;
    private final TargetService targetService;
    private final Clock clock;
    private final ZoneId zone;

    public DashboardService(MetricsRepository metrics, SalesTargetRepository targets, TargetService targetService,
                            Clock clock, AppProperties props) {
        this.metrics = metrics;
        this.targets = targets;
        this.targetService = targetService;
        this.clock = clock;
        this.zone = props.businessZone();
    }

    public Period period(LocalDate from, LocalDate to) {
        return Period.resolve(from, to, LocalDate.now(clock.withZone(zone)), zone);
    }

    public DashboardSummary summary(AuthenticatedUser caller, LocalDate from, LocalDate to, Long branchId) {
        Period p = period(from, to);
        MetricScope scope = MetricScope.of(caller, branchId, null);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Period thisMonth = new Period(YearMonth.from(today).atDay(1), YearMonth.from(today).atEndOfMonth(), zone);

        List<TargetResponse> inScope = targetsForKpi(caller, scope, p);
        BigDecimal achievement = inScope.isEmpty() ? null : inScope.stream().map(TargetResponse::achievementPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(inScope.size()), 1, RoundingMode.HALF_UP);

        return new DashboardSummary(p.from(), p.to(),
                metrics.totalCustomers(scope),
                metrics.newCustomers(scope, thisMonth),
                metrics.openLeads(scope),
                metrics.convertedLeads(scope, p),
                metrics.completedSalesValue(scope, p, null),
                metrics.completedSalesValue(scope, p, "DEPOSIT"),
                achievement, inScope.size(),
                metrics.overdueFollowUps(scope, MetricsRepository.utc(clock.instant())));
    }

    /** Officers: their own targets. Managers/admin: branch-level targets if any exist, otherwise all officer targets. */
    private List<TargetResponse> targetsForKpi(AuthenticatedUser caller, MetricScope scope, Period p) {
        var rows = targets.findOverlapping(p.from(), p.to(), scope.branchId(),
                        caller.role() == Role.RELATIONSHIP_OFFICER ? caller.id() : null, false).stream()
                .map(t -> targetService.toResponse(caller, t)).toList();
        if (caller.role() == Role.RELATIONSHIP_OFFICER) {
            return rows;
        }
        var branchLevel = rows.stream().filter(t -> t.user() == null).toList();
        return branchLevel.isEmpty() ? rows : branchLevel;
    }

    /** Monthly completed sales (value + count + deposit value), with zero-filled months. */
    public List<Map<String, Object>> salesTrend(AuthenticatedUser caller, LocalDate from, LocalDate to, Long branchId) {
        Period p = period(from, to);
        return fillMonths(p, metrics.salesByMonth(MetricScope.of(caller, branchId, null), p),
                List.of("completedValue", "completedCount", "depositValue"));
    }

    public List<Map<String, Object>> customerAcquisition(AuthenticatedUser caller, LocalDate from, LocalDate to, Long branchId) {
        Period p = period(from, to);
        return fillMonths(p, metrics.customersByMonth(MetricScope.of(caller, branchId, null), p),
                List.of("newCustomers", "MASS", "EMERGING_AFFLUENT", "AFFLUENT", "SME"));
    }

    /** Leads created in the range by current stage, in pipeline order (every stage present). */
    public List<Map<String, Object>> leadFunnel(AuthenticatedUser caller, LocalDate from, LocalDate to, Long branchId) {
        Period p = period(from, to);
        Map<String, Map<String, Object>> byStage = new HashMap<>();
        metrics.leadsByStage(MetricScope.of(caller, branchId, null), p).forEach(r -> byStage.put((String) r.get("stage"), r));
        List<Map<String, Object>> out = new ArrayList<>();
        for (LeadStatus s : LeadStatus.values()) {
            Map<String, Object> r = byStage.get(s.name());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stage", s.name());
            row.put("leads", r == null ? 0L : ((Number) r.get("leads")).longValue());
            row.put("estimatedValue", r == null ? BigDecimal.ZERO : r.get("estimatedValue"));
            out.add(row);
        }
        return out;
    }

    public List<Map<String, Object>> productMix(AuthenticatedUser caller, LocalDate from, LocalDate to, Long branchId) {
        Period p = period(from, to);
        return metrics.salesByProduct(MetricScope.of(caller, branchId, null), p);
    }

    /** Managers and admin only: one row per officer, ranked by completed sales value. */
    public List<Map<String, Object>> officerPerformance(AuthenticatedUser caller, LocalDate from, LocalDate to, Long branchId) {
        if (caller.role() == Role.RELATIONSHIP_OFFICER) {
            throw new ForbiddenException("Team performance is available to managers.");
        }
        Period p = period(from, to);
        MetricScope scope = MetricScope.of(caller, branchId, null);
        List<Map<String, Object>> rows = metrics.officerActivity(scope, p, MetricsRepository.utc(clock.instant()));
        Map<Long, List<TargetResponse>> targetsByOfficer = new HashMap<>();
        targets.findOverlapping(p.from(), p.to(), scope.branchId(), null, false).stream()
                .filter(t -> t.getUser() != null)
                .forEach(t -> targetsByOfficer.computeIfAbsent(t.getUser().getId(), k -> new ArrayList<>())
                        .add(targetService.toResponse(caller, t)));
        for (Map<String, Object> row : rows) {
            Long officerId = ((Number) row.get("officerId")).longValue();
            List<TargetResponse> list = targetsByOfficer.getOrDefault(officerId, List.of());
            row.put("targetAchievementPct", list.isEmpty() ? null : list.stream().map(TargetResponse::achievementPct)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_UP));
        }
        return rows;
    }

    /** Target vs achievement per category for the caller's targets overlapping the range. */
    public List<TargetResponse> targetProgress(AuthenticatedUser caller, LocalDate from, LocalDate to, Long branchId) {
        Period p = period(from, to);
        return targetService.list(caller, p.from(), p.to(), branchId, null);
    }

    private static List<Map<String, Object>> fillMonths(Period p, List<Map<String, Object>> rows, List<String> fields) {
        Map<String, Map<String, Object>> byMonth = new HashMap<>();
        rows.forEach(r -> byMonth.put((String) r.get("month"), r));
        List<Map<String, Object>> out = new ArrayList<>();
        for (String month : p.months()) {
            Map<String, Object> src = byMonth.get(month);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            for (String f : fields) {
                Object v = src == null ? null : src.get(f);
                row.put(f, v == null ? 0 : v);
            }
            out.add(row);
        }
        return out;
    }
}
