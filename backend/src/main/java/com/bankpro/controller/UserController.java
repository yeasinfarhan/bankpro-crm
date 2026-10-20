package com.bankpro.controller;

import com.bankpro.dto.UserResponse;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUser;

    public UserController(UserService userService, CurrentUserProvider currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    /** Layer 1: role check here. Layer 2: branch scope inside UserService. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public List<UserResponse> listTeam() {
        return userService.listTeam(currentUser.get());
    }

    /** Officers and managers who can be assigned customers/leads. Available to every signed-in role. */
    @GetMapping("/assignable")
    public List<UserResponse> assignable(@RequestParam(required = false) Long branchId) {
        return userService.listAssignable(currentUser.get(), branchId);
    }
}
