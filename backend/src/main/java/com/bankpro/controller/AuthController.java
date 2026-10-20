package com.bankpro.controller;

import com.bankpro.dto.LoginRequest;
import com.bankpro.dto.LoginResponse;
import com.bankpro.dto.UserResponse;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.AuthService;
import com.bankpro.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controllers stay thin: validate input (@Valid), call a service, return a DTO.
 * Business rules live in services so they can be tested without HTTP.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CurrentUserProvider currentUser;

    public AuthController(AuthService authService, UserService userService, CurrentUserProvider currentUser) {
        this.authService = authService;
        this.userService = userService;
        this.currentUser = currentUser;
    }

    /** Public. Returns a JWT access token. Behind a proxy, the client IP is resolved by forward-headers-strategy. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    @GetMapping("/me")
    public UserResponse me() {
        return userService.me(currentUser.get());
    }
}
