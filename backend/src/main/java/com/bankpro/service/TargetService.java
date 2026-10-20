package com.bankpro.service;

import com.bankpro.audit.AuditService;
import com.bankpro.config.AppProperties;
import com.bankpro.dto.TargetRequest;
import com.bankpro.dto.TargetResponse;
import com.bankpro.entity.AppUser;
import com.bankpro.entity.PeriodType;
import com.bankpro.entity.Role;
import com.bankpro.entity.SalesTarget;
import com.bankpro.exception.ConflictException;
import com.bankpro.exception.ForbiddenException;
import com.bankpro.exception.InvalidFieldException;
import com.bankpro.exception.NotFoundException;
import com.bankpro.mapper.CustomerMapper;
import com.bankpro.mapper.UserMapper;
import com.bankpro.metrics.MetricsRepository;
import com.bankpro.metrics.Period;
import com.bankpro.repository.AppUserRepository;
import com.bankpro.repository.BranchRepository;
import com.bankpro.repository.SalesTargetRepository;
import com.bankpro.security.AuthenticatedUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Sales targets. Managers set them for their branch or its officers; everyone sees progress computed live.
 * Achievement % = actual ÷ target × 100. Targets must be > 0 (database CHECK + validation), so there is
 * never a division by zero; "no target" simply means no row (shown as "—").
 */
@Service
@Transactional
public class TargetService {

    private static final Set<Role> OWNER_ROLES = Set.of(Role.RELATIONSHIP_OFFICER, Role.BRANCH_MANAGER);

    private final SalesTargetRepository targets;
    private final AppUserRepository users;
    private final BranchRepository branches;
    private final MetricsRepository metrics;
    private final AuditService audit;
    private final ZoneId zone;

    public TargetService(SalesTargetRepository targets, AppUserRepository users, BranchRepository branches,
                         MetricsRepository metrics, AuditService audit, AppProperties props) {
        this.targets = targets;
        this.users = users;
        this.branches = branches;
        this.metrics = metrics;
        this.audit = audit;
        this.zone = props.businessZone();
    }

    /** Targets overlapping the range, with live actuals. Officers: their own + their branch's targets. */
    @Transactional(readOnly = true)
    public List<TargetResponse> list(AuthenticatedUser caller, LocalDate from, LocalDate to, Long branchId, Long userId) {
        Long branch = caller.role() == Role.ADMIN ? branchId : caller.branchId();
        Long user = caller.role() == Role.RELATIONSHIP_OFFICER ? caller.id() : userId;
        boolean includeBranch = caller.role() == Role.RELATIONSHIP_OFFICER;
        return targets.findOverlapping(from, to, branch, user, includeBranch).stream()
                .map(t -> toResponse(caller, t)).toList();
    }

    /** Officers ranked by average achievement across their targets in the range. */
    @Transactional(readOnly = true)
    public List<TargetResponse.Ranking> ranking(AuthenticatedUser caller, LocalDate from, LocalDate to, Long branchId) {
        if (caller.role() == Role.RELATIONSHIP_OFFICER) {
            throw new ForbiddenException("Team rankings are available to managers.");
        }
        Map<Long, List<TargetResponse>> byOfficer = new LinkedHashMap<>();
        for (TargetResponse t : list(caller, from, to, branchId, null)) {
            if (t.user() != null) {
                byOfficer.computeIfAbsent(t.user().id(), k -> new ArrayList<>()).add(t);
            }
        }
        List<TargetResponse.Ranking> rows = new ArrayList<>();
        byOfficer.forEach((id, list) -> {
            BigDecimal avg = list.stream().map(TargetResponse::achievementPct).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_UP);
            int met = (int) list.stream().filter(t -> t.achievementPct().compareTo(BigDecimal.valueOf(100)) >= 0).count();
            rows.add(new TargetResponse.Ranking(0, list.get(0).user(), list.size(), avg, met));
        });
        rows.sort(Comparator.comparing(TargetResponse.Ranking::averageAchievementPct).reversed()
                .thenComparing(r -> r.officer().fullName()));
        List<TargetResponse.Ranking> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            var r = rows.get(i);
            ranked.add(new TargetResponse.Ranking(i + 1, r.officer(), r.targets(), r.averageAchievementPct(), r.targetsMet()));
        }
        return ranked;
    }

    public TargetResponse create(AuthenticatedUser caller, TargetRequest req, String ip) {
        requireManager(caller);
        LocalDate start = periodStart(req.periodType(), req.periodStart());
        LocalDate end = periodEnd(req.periodType(), start);
        requireWholeNumberForCounts(req);
        AppUser user = null;
        if (req.userId() != null) {
            user = users.findWithBranchById(req.userId())
                    .filter(AppUser::isActive)
                    .filter(u -> OWNER_ROLES.contains(u.getRole()))
                    .filter(u -> u.getBranch() != null && Objects.equals(u.getBranch().getId(), caller.branchId()))
                    .orElseThrow(() -> new InvalidFieldException("userId", "Choose an active officer from your branch."));
        }
        // Checked first so a normal duplicate is a clear 409, not a database error in the logs;
        // the unique key still guarantees it if two requests race.
        if (targets.existsSame(caller.branchId(), req.userId(), req.periodType(), start, req.category())) {
            throw duplicate(req);
        }
        SalesTarget target = new SalesTarget(branches.getReferenceById(caller.branchId()), user, req.periodType(),
                start, end, req.category(), req.targetValue(), users.getReferenceById(caller.id()));
        try {
            targets.saveAndFlush(target);
        } catch (DataIntegrityViolationException e) {
            throw duplicate(req);
        }
        audit.record(caller.id(), "TARGET_CREATED", "TARGET", target.getId(),
                Map.of("category", req.category().name(), "periodStart", start.toString(), "value", req.targetValue()), ip);
        return toResponse(caller, targets.findWithDetailsById(target.getId()).orElseThrow());
    }

    private static ConflictException duplicate(TargetRequest req) {
        return new ConflictException("A " + req.category().name().toLowerCase().replace('_', ' ')
                + " target for this person and period already exists. Edit it instead.");
    }

    /** Only the value can change; owner, category and period define the target's identity. */
    public TargetResponse update(AuthenticatedUser caller, Long id, BigDecimal value, String ip) {
        requireManager(caller);
        SalesTarget t = findOwnBranch(caller, id);
        if (value == null || value.signum() <= 0) {
            throw new InvalidFieldException("targetValue", "Target must be greater than zero");
        }
        if (!t.getCategory().isMoney() && value.stripTrailingZeros().scale() > 0) {
            throw new InvalidFieldException("targetValue", "This target counts items — use a whole number.");
        }
        BigDecimal old = t.getTargetValue();
        t.changeValue(value);
        audit.record(caller.id(), "TARGET_UPDATED", "TARGET", id, Map.of("from", old, "to", value), ip);
        return toResponse(caller, t);
    }

    /** Targets are plans, not business history, so a mistaken one may be removed (audit-logged). */
    public void delete(AuthenticatedUser caller, Long id, String ip) {
        requireManager(caller);
        SalesTarget t = findOwnBranch(caller, id);
        targets.delete(t);
        audit.record(caller.id(), "TARGET_DELETED", "TARGET", id,
                Map.of("category", t.getCategory().name(), "periodStart", t.getPeriodStart().toString()), ip);
    }

    // ---------------------------------------------------------------- helpers

    TargetResponse toResponse(AuthenticatedUser caller, SalesTarget t) {
        Period period = new Period(t.getPeriodStart(), t.getPeriodEnd(), zone);
        Long userId = t.getUser() == null ? null : t.getUser().getId();
        BigDecimal actual = metrics.targetActual(t.getCategory().name(), t.getBranch().getId(), userId, period);
        return new TargetResponse(t.getId(), UserMapper.toResponse(t.getBranch()), CustomerMapper.summary(t.getUser()),
                t.getPeriodType(), t.getPeriodStart(), t.getPeriodEnd(), t.getCategory(),
                t.getCategory().isMoney() ? "BDT" : "COUNT", t.getTargetValue(), actual,
                achievement(actual, t.getTargetValue()), t.getTargetValue().subtract(actual).max(BigDecimal.ZERO),
                caller.role() == Role.BRANCH_MANAGER && Objects.equals(caller.branchId(), t.getBranch().getId()));
    }

    /** actual ÷ target × 100, one decimal. Returns null for a missing or non-positive target (no divide-by-zero). */
    public static BigDecimal achievement(BigDecimal actual, BigDecimal target) {
        if (target == null || target.signum() <= 0) {
            return null;
        }
        return actual.multiply(BigDecimal.valueOf(100)).divide(target, 1, RoundingMode.HALF_UP);
    }

    static LocalDate periodStart(PeriodType type, LocalDate any) {
        if (type == PeriodType.MONTHLY) {
            return any.withDayOfMonth(1);
        }
        int firstMonthOfQuarter = ((any.getMonthValue() - 1) / 3) * 3 + 1;
        return LocalDate.of(any.getYear(), firstMonthOfQuarter, 1);
    }

    static LocalDate periodEnd(PeriodType type, LocalDate start) {
        return start.plusMonths(type == PeriodType.MONTHLY ? 1 : 3).minusDays(1);
    }

    private static void requireWholeNumberForCounts(TargetRequest req) {
        if (!req.category().isMoney() && req.targetValue().stripTrailingZeros().scale() > 0) {
            throw new InvalidFieldException("targetValue", "This target counts items — use a whole number.");
        }
    }

    private SalesTarget findOwnBranch(AuthenticatedUser caller, Long id) {
        return targets.findWithDetailsById(id)
                .filter(t -> Objects.equals(t.getBranch().getId(), caller.branchId()))
                .orElseThrow(() -> new NotFoundException("Target not found."));
    }

    private static void requireManager(AuthenticatedUser caller) {
        if (caller.role() != Role.BRANCH_MANAGER) {
            throw new ForbiddenException("Only branch managers can set sales targets.");
        }
    }
}
