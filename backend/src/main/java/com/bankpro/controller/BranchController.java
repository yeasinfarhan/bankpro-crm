package com.bankpro.controller;

import com.bankpro.dto.BranchResponse;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.BranchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;
    private final CurrentUserProvider currentUser;

    public BranchController(BranchService branchService, CurrentUserProvider currentUser) {
        this.branchService = branchService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<BranchResponse> list() {
        return branchService.listVisible(currentUser.get());
    }
}
