package com.bankpro.controller;

import com.bankpro.dto.CustomerRequest;
import com.bankpro.dto.CustomerResponse;
import com.bankpro.dto.InteractionRequest;
import com.bankpro.dto.TimelineItem;
import com.bankpro.service.InteractionService;
import com.bankpro.dto.PageResponse;
import com.bankpro.entity.CustomerSegment;
import com.bankpro.entity.CustomerStatus;
import com.bankpro.report.CsvWriter;
import com.bankpro.repository.CustomerSpecifications;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final InteractionService interactionService;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public CustomerController(CustomerService customerService, InteractionService interactionService,
                              CurrentUserProvider currentUser, Clock clock) {
        this.customerService = customerService;
        this.interactionService = interactionService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /** Chronological history: interactions, scheduled follow-ups, leads and simulated sales (newest first). */
    @GetMapping("/{id}/timeline")
    public List<TimelineItem> timeline(@PathVariable Long id) {
        return interactionService.timeline(currentUser.get(), id);
    }

    /** Log an interaction that already happened (call, meeting, note ...). */
    @PostMapping("/{id}/interactions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public TimelineItem recordInteraction(@PathVariable Long id, @Valid @RequestBody InteractionRequest request) {
        return interactionService.record(currentUser.get(), id, request);
    }

    /** Server-side search, filtering, sorting and pagination. Example: ?q=rahman&segment=AFFLUENT&sort=fullName,asc */
    @GetMapping
    public PageResponse<CustomerResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CustomerSegment segment,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String occupation,
            @RequestParam(required = false) Long officerId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        var filter = new CustomerSpecifications.Filter(q, segment, status, occupation, officerId, branchId, includeArchived);
        return customerService.search(currentUser.get(), filter, page, size, sort);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.get(currentUser.get(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request, HttpServletRequest http) {
        return customerService.create(currentUser.get(), request, http.getRemoteAddr());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'RELATIONSHIP_OFFICER')")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request,
                                   HttpServletRequest http) {
        return customerService.update(currentUser.get(), id, request, http.getRemoteAddr());
    }

    /** Archives (soft-deletes). Business history is never erased (DECISIONS.md D-13). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public void archive(@PathVariable Long id, HttpServletRequest http) {
        customerService.archive(currentUser.get(), id, http.getRemoteAddr());
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public CustomerResponse restore(@PathVariable Long id, HttpServletRequest http) {
        return customerService.restore(currentUser.get(), id, http.getRemoteAddr());
    }

    /** CSV of the caller's exportable customers with the same filters as the list. Formula-injection safe. */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String q,
                       @RequestParam(required = false) CustomerSegment segment,
                       @RequestParam(required = false) CustomerStatus status,
                       @RequestParam(required = false) String occupation,
                       @RequestParam(required = false) Long officerId,
                       @RequestParam(required = false) Long branchId,
                       @RequestParam(defaultValue = "false") boolean includeArchived,
                       HttpServletRequest http, HttpServletResponse response) throws IOException {
        var filter = new CustomerSpecifications.Filter(q, segment, status, occupation, officerId, branchId, includeArchived);
        List<CustomerResponse> rows = customerService.exportRows(currentUser.get(), filter, http.getRemoteAddr());

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"bankpro-customers-" + LocalDate.now(clock) + ".csv\"");
        Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        CsvWriter csv = new CsvWriter(writer).writeBom();
        csv.row(List.of("Customer ID", "Full name", "Email", "Phone", "Occupation", "Employment type", "Income range",
                "City", "Segment", "Status", "Branch", "Relationship officer", "Created (UTC)", "Last interaction (UTC)"));
        for (CustomerResponse c : rows) {
            csv.row(java.util.Arrays.asList(c.code(), c.fullName(), c.email(), c.phone(), c.occupation(),
                    c.employmentType(), c.incomeRange(), c.city(), c.segment(), c.status(),
                    c.branch() == null ? null : c.branch().name(),
                    c.assignedOfficer() == null ? null : c.assignedOfficer().fullName(),
                    c.createdAt(), c.lastInteractionAt()));
        }
        writer.flush();
    }
}
