package com.bankpro.service;

import com.bankpro.audit.AuditAction;
import com.bankpro.audit.AuditService;
import com.bankpro.config.AppProperties;
import com.bankpro.dto.CustomerRequest;
import com.bankpro.dto.CustomerResponse;
import com.bankpro.dto.PageResponse;
import com.bankpro.entity.AppUser;
import com.bankpro.entity.BankingProduct;
import com.bankpro.entity.Branch;
import com.bankpro.entity.Customer;
import com.bankpro.entity.CustomerProduct;
import com.bankpro.entity.CustomerStatus;
import com.bankpro.entity.Role;
import com.bankpro.exception.ConflictException;
import com.bankpro.exception.FieldConflictException;
import com.bankpro.exception.ForbiddenException;
import com.bankpro.exception.InvalidFieldException;
import com.bankpro.exception.NotFoundException;
import com.bankpro.mapper.CustomerMapper;
import com.bankpro.repository.AppUserRepository;
import com.bankpro.repository.BankingProductRepository;
import com.bankpro.repository.BranchRepository;
import com.bankpro.repository.CustomerProductRepository;
import com.bankpro.repository.CustomerRepository;
import com.bankpro.repository.CustomerSpecifications;
import com.bankpro.security.AuthenticatedUser;
import com.bankpro.security.DataScope;
import com.bankpro.validation.ContactNormalizer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Customer management with branch- and ownership-based rules (DECISIONS.md D-08, D-09):
 * <ul>
 *   <li>View: ADMIN all branches (read-only); managers and officers their whole branch.</li>
 *   <li>Edit: managers anything in their branch; officers only customers assigned to them.</li>
 *   <li>Archive/restore: managers only. Archiving keeps all history (soft delete).</li>
 *   <li>CSV export: ADMIN all, managers their branch, officers only their own customers.</li>
 * </ul>
 * A record outside the caller's scope is reported as 404 so IDs cannot be probed.
 */
@Service
@Transactional
public class CustomerService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_EXPORT_ROWS = 5000;
    private static final Set<String> SORTABLE = Set.of(
            "fullName", "createdAt", "updatedAt", "lastInteractionAt", "segment", "status", "city");
    private static final Set<Role> OWNER_ROLES = Set.of(Role.RELATIONSHIP_OFFICER, Role.BRANCH_MANAGER);

    private final CustomerRepository customers;
    private final CustomerProductRepository holdings;
    private final BankingProductRepository products;
    private final AppUserRepository users;
    private final BranchRepository branches;
    private final AuditService audit;
    private final Clock clock;
    private final AppProperties props;

    public CustomerService(CustomerRepository customers, CustomerProductRepository holdings,
                           BankingProductRepository products, AppUserRepository users, BranchRepository branches,
                           AuditService audit, Clock clock, AppProperties props) {
        this.customers = customers;
        this.holdings = holdings;
        this.products = products;
        this.users = users;
        this.branches = branches;
        this.audit = audit;
        this.clock = clock;
        this.props = props;
    }

    // ---------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(AuthenticatedUser caller, CustomerSpecifications.Filter filter,
                                                 int page, int size, String sort) {
        DataScope scope = DataScope.branchRead(caller);
        var result = customers.findAll(CustomerSpecifications.matching(scope, filter), pageable(page, size, sort));
        return PageResponse.of(result, c -> CustomerMapper.toResponse(c, null, permissions(caller, c)));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(AuthenticatedUser caller, Long id) {
        Customer c = findVisible(caller, id);
        return CustomerMapper.toResponse(c, holdings.findByCustomerIdWithProduct(id), permissions(caller, c));
    }

    /** Rows for CSV export, using the stricter export scope (officers: own customers only). */
    @Transactional(readOnly = true)
    public List<CustomerResponse> exportRows(AuthenticatedUser caller, CustomerSpecifications.Filter filter, String ip) {
        DataScope scope = DataScope.of(caller);
        var rows = customers.findAll(CustomerSpecifications.matching(scope, filter),
                        PageRequest.of(0, MAX_EXPORT_ROWS, Sort.by("fullName").and(Sort.by("id"))))
                .map(c -> CustomerMapper.toResponse(c, null, null)).getContent();
        Map<String, Object> details = new HashMap<>();
        details.put("rows", rows.size());
        details.put("scope", scope.level().name());
        audit.record(caller.id(), AuditAction.CUSTOMERS_EXPORTED, "CUSTOMER", null, details, ip);
        return rows;
    }

    // ---------------------------------------------------------------- write

    public CustomerResponse create(AuthenticatedUser caller, CustomerRequest req, String ip) {
        requireRole(caller, Role.BRANCH_MANAGER, Role.RELATIONSHIP_OFFICER);
        Branch branch = branches.findById(caller.branchId())
                .orElseThrow(() -> new IllegalStateException("Branch missing for user " + caller.id()));
        AppUser creator = users.getReferenceById(caller.id());

        Normalized n = normalize(req);
        assertNoDuplicate(n, null);

        Customer c = new Customer(branch, creator);
        c.updateProfile(n.fullName, n.email, n.phone, n.occupation, req.employmentType(), req.incomeRange(), n.city,
                req.segment());
        c.assignTo(resolveOwner(caller, req.assignedOfficerId(), null));
        c.changeStatus(editableStatus(req.status(), CustomerStatus.PROSPECT));
        customers.save(c);

        Set<Long> productIds = replaceManualHoldings(c, req.productIds());
        if (!productIds.isEmpty() && c.getStatus() == CustomerStatus.PROSPECT) {
            c.changeStatus(CustomerStatus.ACTIVE); // holding a product makes them an active customer
        }
        audit.record(caller.id(), AuditAction.CUSTOMER_CREATED, "CUSTOMER", c.getId(),
                Map.of("code", CustomerMapper.code(c.getId())), ip);
        return CustomerMapper.toResponse(c, holdings.findByCustomerIdWithProduct(c.getId()), permissions(caller, c));
    }

    public CustomerResponse update(AuthenticatedUser caller, Long id, CustomerRequest req, String ip) {
        Customer c = findVisible(caller, id);
        if (!permissions(caller, c).canEdit()) {
            if (c.isArchived()) {
                throw new ConflictException("This customer is archived. Restore it before editing.");
            }
            throw new ForbiddenException("You can only edit customers assigned to you.");
        }
        if (req.version() == null) {
            throw new InvalidFieldException("version", "Version is required when updating.");
        }
        if (req.version() != c.getVersion()) {
            throw new ConflictException("This customer was changed by someone else. Reload and try again.");
        }

        Normalized n = normalize(req);
        assertNoDuplicate(n, c.getId());
        c.updateProfile(n.fullName, n.email, n.phone, n.occupation, req.employmentType(), req.incomeRange(), n.city,
                req.segment());
        if (caller.role() == Role.BRANCH_MANAGER) {
            c.assignTo(resolveOwner(caller, req.assignedOfficerId(), c));
        }
        c.changeStatus(editableStatus(req.status(), c.getStatus()));
        holdings.deleteManualByCustomerId(c.getId());
        replaceManualHoldings(c, req.productIds());

        audit.record(caller.id(), AuditAction.CUSTOMER_UPDATED, "CUSTOMER", c.getId(),
                Map.of("code", CustomerMapper.code(c.getId())), ip);
        customers.flush(); // apply the version increment now so the response carries the new version
        return CustomerMapper.toResponse(c, holdings.findByCustomerIdWithProduct(c.getId()), permissions(caller, c));
    }

    /** Soft delete: the customer disappears from default lists but leads, sales and history are kept. */
    public void archive(AuthenticatedUser caller, Long id, String ip) {
        requireRole(caller, Role.BRANCH_MANAGER);
        Customer c = findVisible(caller, id);
        if (c.isArchived()) {
            return; // idempotent
        }
        c.archive(clock.instant());
        audit.record(caller.id(), AuditAction.CUSTOMER_ARCHIVED, "CUSTOMER", id,
                Map.of("code", CustomerMapper.code(id)), ip);
    }

    public CustomerResponse restore(AuthenticatedUser caller, Long id, String ip) {
        requireRole(caller, Role.BRANCH_MANAGER);
        Customer c = findVisible(caller, id);
        if (c.isArchived()) {
            c.restore();
            if (holdings.existsByCustomerId(id)) {
                c.changeStatus(CustomerStatus.ACTIVE);
            }
            audit.record(caller.id(), AuditAction.CUSTOMER_RESTORED, "CUSTOMER", id,
                    Map.of("code", CustomerMapper.code(id)), ip);
        }
        return CustomerMapper.toResponse(c, holdings.findByCustomerIdWithProduct(id), permissions(caller, c));
    }

    // ---------------------------------------------------------------- rules

    /** Loads a customer only if the caller may see it; otherwise 404 (never 403) so IDs cannot be probed. */
    Customer findVisible(AuthenticatedUser caller, Long id) {
        DataScope scope = DataScope.branchRead(caller);
        return customers.findWithDetailsById(id)
                .filter(c -> scope.allows(c.getBranch().getId(), ownerId(c)))
                .orElseThrow(() -> new NotFoundException("Customer not found."));
    }

    CustomerResponse.Permissions permissions(AuthenticatedUser caller, Customer c) {
        boolean sameBranch = Objects.equals(caller.branchId(), c.getBranch().getId());
        boolean manager = caller.role() == Role.BRANCH_MANAGER && sameBranch;
        boolean owner = caller.role() == Role.RELATIONSHIP_OFFICER && sameBranch
                && Objects.equals(caller.id(), ownerId(c));
        return new CustomerResponse.Permissions(!c.isArchived() && (manager || owner), manager);
    }

    /**
     * Officers always own what they create. Managers may pick any active officer/manager of their own branch,
     * or leave it unassigned.
     */
    private AppUser resolveOwner(AuthenticatedUser caller, Long requestedId, Customer existing) {
        if (caller.role() == Role.RELATIONSHIP_OFFICER) {
            return existing != null ? existing.getAssignedOfficer() : users.getReferenceById(caller.id());
        }
        if (requestedId == null) {
            return null;
        }
        return users.findWithBranchById(requestedId)
                .filter(AppUser::isActive)
                .filter(u -> OWNER_ROLES.contains(u.getRole()))
                .filter(u -> u.getBranch() != null && Objects.equals(u.getBranch().getId(), caller.branchId()))
                .orElseThrow(() -> new InvalidFieldException("assignedOfficerId",
                        "Choose an active officer from your branch."));
    }

    private static CustomerStatus editableStatus(CustomerStatus requested, CustomerStatus fallback) {
        if (requested == null) {
            return fallback;
        }
        if (requested == CustomerStatus.ARCHIVED) {
            throw new InvalidFieldException("status", "Use the archive action to archive a customer.");
        }
        return requested;
    }

    private Set<Long> replaceManualHoldings(Customer c, List<Long> requested) {
        Set<Long> ids = requested == null ? Set.of() : new LinkedHashSet<>(requested.stream().filter(Objects::nonNull).toList());
        if (ids.isEmpty()) {
            return ids;
        }
        Map<Long, BankingProduct> found = products.findAllById(ids).stream()
                .collect(Collectors.toMap(BankingProduct::getId, p -> p));
        if (found.size() != ids.size()) {
            throw new InvalidFieldException("productIds", "One or more selected products do not exist.");
        }
        Set<Long> alreadyFromSales = holdings.findByCustomerIdWithProduct(c.getId()).stream()
                .map(h -> h.getProduct().getId()).collect(Collectors.toSet());
        LocalDate today = LocalDate.now(clock.withZone(props.businessZone()));
        ids.stream().filter(pid -> !alreadyFromSales.contains(pid))
                .forEach(pid -> holdings.save(new CustomerProduct(c, found.get(pid), today, null)));
        return ids;
    }

    private void assertNoDuplicate(Normalized n, Long selfId) {
        customers.findIdByPhone(n.phone).filter(id -> !id.equals(selfId)).ifPresent(id -> {
            throw new FieldConflictException("phone", "A customer with this phone number already exists.");
        });
        if (n.email != null) {
            customers.findIdByEmail(n.email).filter(id -> !id.equals(selfId)).ifPresent(id -> {
                throw new FieldConflictException("email", "A customer with this email already exists.");
            });
        }
    }

    private static Normalized normalize(CustomerRequest req) {
        String phone = ContactNormalizer.normalizePhone(req.phone()).orElseThrow(() -> new InvalidFieldException(
                "phone", "Enter a valid Bangladesh mobile number, e.g. 01712345678."));
        return new Normalized(ContactNormalizer.clean(req.fullName()), ContactNormalizer.normalizeEmail(req.email()),
                phone, ContactNormalizer.clean(req.occupation()), ContactNormalizer.clean(req.city()));
    }

    private static Pageable pageable(int page, int size, String sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort order = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (!SORTABLE.contains(parts[0])) {
                throw new InvalidFieldException("sort", "Cannot sort by '" + parts[0] + "'.");
            }
            Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            order = Sort.by(dir, parts[0]);
        }
        return PageRequest.of(Math.max(page, 0), safeSize, order.and(Sort.by("id")));
    }

    private static Long ownerId(Customer c) {
        return c.getAssignedOfficer() == null ? null : c.getAssignedOfficer().getId();
    }

    private static void requireRole(AuthenticatedUser caller, Role... allowed) {
        for (Role r : allowed) {
            if (caller.role() == r) {
                return;
            }
        }
        throw new ForbiddenException("You do not have permission to perform this action.");
    }

    private record Normalized(String fullName, String email, String phone, String occupation, String city) {
    }
}
