package com.bankpro.service;

import com.bankpro.audit.AuditAction;
import com.bankpro.audit.AuditService;
import com.bankpro.config.AppProperties;
import com.bankpro.dto.ConversionResponse;
import com.bankpro.dto.LeadRequest;
import com.bankpro.dto.LeadResponse;
import com.bankpro.dto.LeadStatusRequest;
import com.bankpro.dto.LeadSummaryResponse;
import com.bankpro.dto.PageResponse;
import com.bankpro.dto.SaleDecisionRequest;
import com.bankpro.dto.SaleResponse;
import com.bankpro.entity.AppUser;
import com.bankpro.entity.BankingProduct;
import com.bankpro.entity.Customer;
import com.bankpro.entity.CustomerProduct;
import com.bankpro.entity.CustomerStatus;
import com.bankpro.entity.Lead;
import com.bankpro.entity.LeadPriority;
import com.bankpro.entity.LeadStatus;
import com.bankpro.entity.ProductCategory;
import com.bankpro.entity.Role;
import com.bankpro.entity.Sale;
import com.bankpro.entity.SaleStatus;
import com.bankpro.exception.BadRequestException;
import com.bankpro.exception.ConflictException;
import com.bankpro.exception.ForbiddenException;
import com.bankpro.exception.InvalidFieldException;
import com.bankpro.exception.NotFoundException;
import com.bankpro.mapper.CustomerMapper;
import com.bankpro.mapper.LeadMapper;
import com.bankpro.repository.AppUserRepository;
import com.bankpro.repository.BankingProductRepository;
import com.bankpro.repository.CustomerProductRepository;
import com.bankpro.repository.LeadAggregates;
import com.bankpro.repository.LeadRepository;
import com.bankpro.repository.LeadSpecifications;
import com.bankpro.repository.SaleRepository;
import com.bankpro.security.AuthenticatedUser;
import com.bankpro.security.DataScope;
import com.bankpro.validation.ContactNormalizer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Leads, the sales pipeline and conversion to simulated sales.
 * <ul>
 *   <li>Scope: officers see only leads assigned to them; managers their branch; ADMIN everything (read-only).</li>
 *   <li>Stage changes go through {@link LeadStageRules}.</li>
 *   <li>Conversion is transactional, locks the lead row and is idempotent: one lead → at most one sale.</li>
 *   <li>Loans and cards convert to PENDING_APPROVAL; only a manager's (simulated) approval makes them COMPLETED.</li>
 * </ul>
 */
@Service
@Transactional
public class LeadService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_BOARD_PER_COLUMN = 50;
    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "updatedAt", "estimatedValue", "expectedCloseDate", "status", "priority", "statusChangedAt");
    private static final Set<ProductCategory> NEEDS_APPROVAL = EnumSet.of(ProductCategory.LOAN, ProductCategory.CARD);
    private static final Set<Role> OWNER_ROLES = Set.of(Role.RELATIONSHIP_OFFICER, Role.BRANCH_MANAGER);

    private final LeadRepository leads;
    private final SaleRepository sales;
    private final BankingProductRepository products;
    private final CustomerProductRepository holdings;
    private final AppUserRepository users;
    private final CustomerService customerService;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;
    private final AppProperties props;

    public LeadService(LeadRepository leads, SaleRepository sales, BankingProductRepository products,
                       CustomerProductRepository holdings, AppUserRepository users, CustomerService customerService,
                       NotificationService notifications, AuditService audit, Clock clock, AppProperties props) {
        this.leads = leads;
        this.sales = sales;
        this.products = products;
        this.holdings = holdings;
        this.users = users;
        this.customerService = customerService;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
        this.props = props;
    }

    // ---------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> search(AuthenticatedUser caller, LeadSpecifications.Filter filter,
                                             int page, int size, String sort) {
        var spec = LeadSpecifications.matching(DataScope.of(caller), filter);
        var result = leads.findAll(spec, pageable(page, size, sort));
        return PageResponse.of(result, l -> toResponse(caller, l, null));
    }

    /** Kanban: every stage with its totals and up to 50 most recently changed cards. */
    @Transactional(readOnly = true)
    public Map<LeadStatus, BoardColumn> board(AuthenticatedUser caller, LeadSpecifications.Filter filter) {
        DataScope scope = DataScope.of(caller);
        var totals = leads.totalsByStatus(LeadSpecifications.matching(scope, filter));
        Map<LeadStatus, BoardColumn> board = new EnumMap<>(LeadStatus.class);
        for (LeadStatus status : LeadStatus.values()) {
            var columnFilter = new LeadSpecifications.Filter(filter.q(), List.of(status), filter.productId(),
                    filter.officerId(), filter.customerId(), filter.branchId(), filter.createdFrom(), filter.createdTo());
            var cards = leads.findAll(LeadSpecifications.matching(scope, columnFilter),
                            PageRequest.of(0, MAX_BOARD_PER_COLUMN, Sort.by(Sort.Direction.DESC, "statusChangedAt", "id")))
                    .map(l -> toResponse(caller, l, null)).getContent();
            var t = totals.get(status);
            board.put(status, new BoardColumn(t.count(), t.estimatedValue(), cards));
        }
        return board;
    }

    public record BoardColumn(long count, BigDecimal estimatedValue, List<LeadResponse> leads) {
    }

    @Transactional(readOnly = true)
    public LeadSummaryResponse summary(AuthenticatedUser caller, LeadSpecifications.Filter filter) {
        var totals = leads.totalsByStatus(LeadSpecifications.matching(DataScope.of(caller), filter));
        Map<LeadStatus, Long> counts = new EnumMap<>(LeadStatus.class);
        Map<LeadStatus, BigDecimal> values = new EnumMap<>(LeadStatus.class);
        totals.forEach((s, t) -> {
            counts.put(s, t.count());
            values.put(s, t.estimatedValue());
        });
        long open = LeadStageRules.OPEN.stream().mapToLong(counts::get).sum();
        BigDecimal openValue = LeadStageRules.OPEN.stream().map(values::get).reduce(BigDecimal.ZERO, BigDecimal::add);
        long converted = counts.get(LeadStatus.CONVERTED);
        long lost = counts.get(LeadStatus.LOST);
        return new LeadSummaryResponse(counts, values, open, openValue, converted, lost, conversionRate(converted, lost));
    }

    /** converted ÷ (converted + lost) × 100, one decimal; null when nothing has closed yet. */
    static BigDecimal conversionRate(long converted, long lost) {
        long closed = converted + lost;
        if (closed == 0) {
            return null;
        }
        return BigDecimal.valueOf(converted * 100L).divide(BigDecimal.valueOf(closed), 1, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public LeadResponse get(AuthenticatedUser caller, Long id) {
        Lead lead = findVisible(caller, id);
        SaleResponse sale = sales.findByLeadId(id)
                .map(s -> LeadMapper.toResponse(s, canDecide(caller, s))).orElse(null);
        return toResponse(caller, lead, sale);
    }

    // ---------------------------------------------------------------- write

    public LeadResponse create(AuthenticatedUser caller, LeadRequest req, String ip) {
        requireRole(caller, Role.BRANCH_MANAGER, Role.RELATIONSHIP_OFFICER);
        if (req.customerId() == null) {
            throw new InvalidFieldException("customerId", "Customer is required");
        }
        Customer customer;
        try {
            customer = customerService.findVisible(caller, req.customerId());
        } catch (NotFoundException e) {
            throw new InvalidFieldException("customerId", "Choose a customer from your branch.");
        }
        if (customer.isArchived()) {
            throw new InvalidFieldException("customerId", "This customer is archived. Restore them first.");
        }
        BankingProduct product = activeProduct(req.productId());
        AppUser officer = resolveOwner(caller, req.assignedOfficerId());

        Lead lead = new Lead(customer, product, officer, users.getReferenceById(caller.id()), clock.instant());
        lead.updateDetails(req.source(), priority(req.priority()), req.estimatedValue(), req.expectedCloseDate(),
                ContactNormalizer.clean(req.notes()));
        leads.save(lead);
        audit.record(caller.id(), AuditAction.LEAD_CREATED, "LEAD", lead.getId(),
                Map.of("code", LeadMapper.code(lead.getId()), "customer", CustomerMapper.code(customer.getId())), ip);
        if (!Objects.equals(officer.getId(), caller.id())) {
            notifyAssigned(lead, officer);
        }
        return toResponse(caller, lead, null);
    }

    public LeadResponse update(AuthenticatedUser caller, Long id, LeadRequest req, String ip) {
        Lead lead = findVisible(caller, id);
        if (!permissions(caller, lead).canEdit()) {
            throw lead.isOpen()
                    ? new ForbiddenException("You can only edit leads assigned to you.")
                    : new ConflictException("Closed leads (converted or lost) cannot be edited.");
        }
        checkVersion(req.version(), lead.getVersion());
        if (!Objects.equals(req.productId(), lead.getProduct().getId())) {
            lead.changeProduct(activeProduct(req.productId()));
        }
        lead.updateDetails(req.source(), priority(req.priority()), req.estimatedValue(), req.expectedCloseDate(),
                ContactNormalizer.clean(req.notes()));
        audit.record(caller.id(), AuditAction.LEAD_UPDATED, "LEAD", id, Map.of("code", LeadMapper.code(id)), ip);
        leads.flush();
        return toResponse(caller, lead, null);
    }

    public LeadResponse changeStatus(AuthenticatedUser caller, Long id, LeadStatusRequest req, String ip) {
        Lead lead = findVisible(caller, id);
        requireOwnerOrManager(caller, lead);
        LeadStatus from = lead.getStatus();
        switch (LeadStageRules.check(from, req.status(), caller.role())) {
            case NO_CHANGE -> {
                return toResponse(caller, lead, null);
            }
            case USE_CONVERT -> throw new BadRequestException("Use the Convert action to record a sale for this lead.");
            case FINAL -> throw new ConflictException("Converted leads are final and cannot change stage.");
            case MANAGER_ONLY -> throw new ForbiddenException("Only a branch manager can reopen a lost lead.");
            case NOT_ALLOWED -> throw new ConflictException(
                    "A lead cannot move from " + from + " to " + req.status() + ". Move one stage at a time.");
            case ALLOWED -> { }
        }
        if (req.status() == LeadStatus.LOST && req.lossReason() == null) {
            throw new InvalidFieldException("lossReason", "Choose why the lead was lost.");
        }
        lead.moveTo(req.status(), clock.instant(), req.lossReason(), ContactNormalizer.clean(req.lossNote()));
        Map<String, Object> details = new HashMap<>();
        details.put("from", from.name());
        details.put("to", req.status().name());
        if (req.lossReason() != null && req.status() == LeadStatus.LOST) {
            details.put("lossReason", req.lossReason().name());
        }
        audit.record(caller.id(), AuditAction.LEAD_STATUS_CHANGED, "LEAD", id, details, ip);
        leads.flush();
        return toResponse(caller, lead, null);
    }

    public LeadResponse assign(AuthenticatedUser caller, Long id, Long officerId, String ip) {
        requireRole(caller, Role.BRANCH_MANAGER);
        Lead lead = findVisible(caller, id);
        if (!lead.isOpen()) {
            throw new ConflictException("Closed leads cannot be reassigned.");
        }
        AppUser officer = resolveOwner(caller, officerId);
        if (!Objects.equals(officer.getId(), lead.getAssignedOfficer().getId())) {
            Long previous = lead.getAssignedOfficer().getId();
            lead.assignTo(officer);
            audit.record(caller.id(), AuditAction.LEAD_ASSIGNED, "LEAD", id,
                    Map.of("from", previous, "to", officer.getId()), ip);
            if (!Objects.equals(officer.getId(), caller.id())) {
                notifyAssigned(lead, officer);
            }
        }
        leads.flush();
        return toResponse(caller, lead, null);
    }

    /**
     * Converts a qualified lead into a simulated sale.
     * <ol>
     *   <li>Lock the lead row (SELECT … FOR UPDATE) — concurrent requests queue up here.</li>
     *   <li>Already converted? Return the existing sale (idempotent, nothing new is created).</li>
     *   <li>Otherwise validate stage, permission and product, then create exactly one sale.</li>
     * </ol>
     * Everything runs in one database transaction: either the sale, the lead change, the customer holding and the
     * audit entry are all saved, or none of them are. UNIQUE(sales.lead_id) is the final safety net.
     */
    public ConversionResponse convert(AuthenticatedUser caller, Long id, BigDecimal amount, String ip) {
        Lead lead = leads.findByIdForUpdate(id)
                .filter(l -> DataScope.of(caller).allows(l.getBranch().getId(), l.getAssignedOfficer().getId()))
                .orElseThrow(() -> new NotFoundException("Lead not found."));

        var existing = sales.findByLeadId(id);
        if (existing.isPresent()) {
            Sale sale = existing.get();
            return new ConversionResponse(toResponse(caller, lead, null),
                    LeadMapper.toResponse(sale, canDecide(caller, sale)), true);
        }
        if (!permissions(caller, lead).canEdit()) {
            throw new ForbiddenException("You can only convert leads assigned to you.");
        }
        if (!LeadStageRules.CONVERTIBLE.contains(lead.getStatus())) {
            throw new ConflictException("Only leads that are Qualified or at Proposal stage can be converted.");
        }
        if (!lead.getProduct().isActive()) {
            throw new ConflictException("This product is no longer active and cannot be sold.");
        }

        Instant now = clock.instant();
        SaleStatus status = NEEDS_APPROVAL.contains(lead.getProduct().getCategory())
                ? SaleStatus.PENDING_APPROVAL : SaleStatus.COMPLETED;
        Sale sale = sales.save(new Sale(lead, amount, status, now));
        lead.moveTo(LeadStatus.CONVERTED, now, null, null);
        if (status == SaleStatus.COMPLETED) {
            recordHolding(sale);
        }
        audit.record(caller.id(), AuditAction.LEAD_CONVERTED, "LEAD", id,
                Map.of("sale", sale.getId(), "status", status.name(), "amount", amount), ip);
        sales.flush(); // surface any constraint violation inside this transaction
        return new ConversionResponse(toResponse(caller, lead, null),
                LeadMapper.toResponse(sale, canDecide(caller, sale)), false);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> listSales(AuthenticatedUser caller, SaleStatus status) {
        DataScope scope = DataScope.of(caller);
        Long branchId = scope.level() == DataScope.Level.ALL ? null : scope.branchId();
        Long officerId = scope.level() == DataScope.Level.OWN ? scope.userId() : null;
        return sales.search(branchId, officerId, status).stream()
                .map(s -> LeadMapper.toResponse(s, canDecide(caller, s))).toList();
    }

    /** A manager records the SIMULATED outcome of a loan/card application. Not a real credit decision. */
    public SaleResponse decide(AuthenticatedUser caller, Long saleId, SaleDecisionRequest req, String ip) {
        requireRole(caller, Role.BRANCH_MANAGER);
        Sale sale = sales.findWithDetailsById(saleId)
                .filter(s -> Objects.equals(s.getBranch().getId(), caller.branchId()))
                .orElseThrow(() -> new NotFoundException("Sale not found."));
        if (sale.getStatus() != SaleStatus.PENDING_APPROVAL) {
            throw new ConflictException("This sale has already been decided.");
        }
        SaleStatus outcome = req.decision() == SaleDecisionRequest.Decision.APPROVE
                ? SaleStatus.COMPLETED : SaleStatus.DECLINED;
        sale.decide(outcome, users.getReferenceById(caller.id()), clock.instant(), ContactNormalizer.clean(req.note()));
        if (outcome == SaleStatus.COMPLETED) {
            recordHolding(sale);
        }
        audit.record(caller.id(), AuditAction.SALE_DECIDED, "SALE", saleId, Map.of("outcome", outcome.name()), ip);
        notifications.notify(sale.getOfficer().getId(), NotificationService.SALE_DECIDED,
                (outcome == SaleStatus.COMPLETED ? "Approved (simulated): " : "Declined (simulated): ")
                        + sale.getProduct().getName() + " for " + sale.getCustomer().getFullName(),
                "/leads/" + sale.getLead().getId(), null);
        return LeadMapper.toResponse(sale, false);
    }

    // ---------------------------------------------------------------- rules & helpers

    Lead findVisible(AuthenticatedUser caller, Long id) {
        DataScope scope = DataScope.of(caller);
        return leads.findWithDetailsById(id)
                .filter(l -> scope.allows(l.getBranch().getId(), l.getAssignedOfficer().getId()))
                .orElseThrow(() -> new NotFoundException("Lead not found."));
    }

    LeadResponse.Permissions permissions(AuthenticatedUser caller, Lead l) {
        boolean sameBranch = Objects.equals(caller.branchId(), l.getBranch().getId());
        boolean manager = caller.role() == Role.BRANCH_MANAGER && sameBranch;
        boolean owner = caller.role() == Role.RELATIONSHIP_OFFICER && sameBranch
                && Objects.equals(caller.id(), l.getAssignedOfficer().getId());
        boolean act = l.isOpen() && (manager || owner);
        return new LeadResponse.Permissions(act, manager && l.isOpen(),
                act && LeadStageRules.CONVERTIBLE.contains(l.getStatus()));
    }

    private LeadResponse toResponse(AuthenticatedUser caller, Lead l, SaleResponse sale) {
        boolean mayMove = caller.role() != Role.ADMIN && (permissions(caller, l).canEdit()
                || (l.getStatus() == LeadStatus.LOST && caller.role() == Role.BRANCH_MANAGER
                && Objects.equals(caller.branchId(), l.getBranch().getId())));
        Set<LeadStatus> allowed = mayMove ? LeadStageRules.nextFor(l.getStatus(), caller.role())
                : EnumSet.noneOf(LeadStatus.class);
        return LeadMapper.toResponse(l, allowed, permissions(caller, l), sale);
    }

    private void requireOwnerOrManager(AuthenticatedUser caller, Lead lead) {
        boolean sameBranch = Objects.equals(caller.branchId(), lead.getBranch().getId());
        boolean manager = caller.role() == Role.BRANCH_MANAGER && sameBranch;
        boolean owner = caller.role() == Role.RELATIONSHIP_OFFICER
                && Objects.equals(caller.id(), lead.getAssignedOfficer().getId());
        if (!manager && !owner) {
            throw new ForbiddenException("You can only change leads assigned to you.");
        }
    }

    private boolean canDecide(AuthenticatedUser caller, Sale s) {
        return caller.role() == Role.BRANCH_MANAGER && s.getStatus() == SaleStatus.PENDING_APPROVAL
                && Objects.equals(caller.branchId(), s.getBranch().getId());
    }

    /** A completed sale means the customer now holds the product; prospects become active customers. */
    private void recordHolding(Sale sale) {
        LocalDate today = LocalDate.now(clock.withZone(props.businessZone()));
        holdings.save(new CustomerProduct(sale.getCustomer(), sale.getProduct(), today, sale.getId()));
        Customer c = sale.getCustomer();
        if (c.getStatus() == CustomerStatus.PROSPECT || c.getStatus() == CustomerStatus.INACTIVE) {
            c.changeStatus(CustomerStatus.ACTIVE);
        }
    }

    private void notifyAssigned(Lead lead, AppUser officer) {
        notifications.notify(officer.getId(), NotificationService.LEAD_ASSIGNED,
                "New lead assigned: " + lead.getProduct().getName() + " for " + lead.getCustomer().getFullName()
                        + " (" + LeadMapper.code(lead.getId()) + ")",
                "/leads/" + lead.getId(), null);
    }

    private AppUser resolveOwner(AuthenticatedUser caller, Long requestedId) {
        if (caller.role() == Role.RELATIONSHIP_OFFICER || requestedId == null) {
            return users.getReferenceById(caller.id());
        }
        return users.findWithBranchById(requestedId)
                .filter(AppUser::isActive)
                .filter(u -> OWNER_ROLES.contains(u.getRole()))
                .filter(u -> u.getBranch() != null && Objects.equals(u.getBranch().getId(), caller.branchId()))
                .orElseThrow(() -> new InvalidFieldException("assignedOfficerId",
                        "Choose an active officer from your branch."));
    }

    private BankingProduct activeProduct(Long productId) {
        return products.findById(productId)
                .filter(BankingProduct::isActive)
                .orElseThrow(() -> new InvalidFieldException("productId", "Choose an active product."));
    }

    private static LeadPriority priority(LeadPriority p) {
        return p == null ? LeadPriority.MEDIUM : p;
    }

    private static void checkVersion(Long requested, long current) {
        if (requested == null) {
            throw new InvalidFieldException("version", "Version is required when updating.");
        }
        if (requested != current) {
            throw new ConflictException("This lead was changed by someone else. Reload and try again.");
        }
    }

    private static Pageable pageable(int page, int size, String sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort order = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (!SORTABLE.contains(parts[0])) {
                throw new InvalidFieldException("sort", "Cannot sort by '" + parts[0] + "'.");
            }
            order = Sort.by(parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
        }
        return PageRequest.of(Math.max(page, 0), safeSize, order.and(Sort.by("id")));
    }

    private static void requireRole(AuthenticatedUser caller, Role... allowed) {
        for (Role r : allowed) {
            if (caller.role() == r) {
                return;
            }
        }
        throw new ForbiddenException("You do not have permission to perform this action.");
    }
}
