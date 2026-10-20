package com.bankpro.controller;

import com.bankpro.dto.DashboardSummary;
import com.bankpro.dto.TargetResponse;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dashboard data. {@code from}/{@code to} are inclusive Bangladesh dates (default: last 6 months).
 * {@code branchId} is honoured for ADMIN only; managers always get their branch, officers their own data.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;
    private final CurrentUserProvider currentUser;

    public DashboardController(DashboardService dashboard, CurrentUserProvider currentUser) {
        this.dashboard = dashboard;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(required = false) Long branchId) {
        return dashboard.summary(currentUser.get(), from, to, branchId);
    }

    @GetMapping("/sales-trend")
    public List<Map<String, Object>> salesTrend(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                @RequestParam(required = false) Long branchId) {
        return dashboard.salesTrend(currentUser.get(), from, to, branchId);
    }

    @GetMapping("/customer-acquisition")
    public List<Map<String, Object>> acquisition(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                 @RequestParam(required = false) Long branchId) {
        return dashboard.customerAcquisition(currentUser.get(), from, to, branchId);
    }

    @GetMapping("/lead-funnel")
    public List<Map<String, Object>> funnel(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @RequestParam(required = false) Long branchId) {
        return dashboard.leadFunnel(currentUser.get(), from, to, branchId);
    }

    @GetMapping("/product-mix")
    public List<Map<String, Object>> productMix(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                @RequestParam(required = false) Long branchId) {
        return dashboard.productMix(currentUser.get(), from, to, branchId);
    }

    @GetMapping("/targets")
    public List<TargetResponse> targets(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(required = false) Long branchId) {
        return dashboard.targetProgress(currentUser.get(), from, to, branchId);
    }

    @GetMapping("/officer-performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public List<Map<String, Object>> officers(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                              @RequestParam(required = false) Long branchId) {
        return dashboard.officerPerformance(currentUser.get(), from, to, branchId);
    }
}
