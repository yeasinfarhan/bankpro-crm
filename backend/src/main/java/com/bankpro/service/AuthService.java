package com.bankpro.service;

import com.bankpro.audit.AuditAction;
import com.bankpro.audit.AuditService;
import com.bankpro.config.AppProperties;
import com.bankpro.dto.LoginRequest;
import com.bankpro.dto.LoginResponse;
import com.bankpro.entity.AppUser;
import com.bankpro.exception.AuthenticationFailedException;
import com.bankpro.exception.TooManyRequestsException;
import com.bankpro.mapper.UserMapper;
import com.bankpro.repository.AppUserRepository;
import com.bankpro.security.JwtService;
import com.bankpro.security.LoginRateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Login logic.
 * Security details worth noticing:
 * <ul>
 *   <li>Unknown email, wrong password and inactive account all return the SAME message, so attackers
 *       cannot discover which emails exist.</li>
 *   <li>For an unknown email we still run a BCrypt check against a dummy hash, so the response time
 *       does not reveal whether the account exists.</li>
 *   <li>Repeated failures for one email + IP are rate-limited (HTTP 429).</li>
 * </ul>
 */
@Service
public class AuthService {

    static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final AuditService audit;
    private final Clock clock;
    private final Duration lockWindow;
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginRateLimiter rateLimiter, AuditService audit, Clock clock, AppProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
        this.clock = clock;
        this.lockWindow = props.loginRateLimit().window();
        this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (rateLimiter.isBlocked(email, ipAddress)) {
            audit.record(null, AuditAction.LOGIN_BLOCKED, "USER", null, Map.of("email", email), ipAddress);
            throw new TooManyRequestsException("Too many failed sign-in attempts. Try again in "
                    + lockWindow.toMinutes() + " minutes.");
        }

        Optional<AppUser> found = users.findByEmail(email);
        boolean passwordOk = passwordEncoder.matches(request.password(),
                found.map(AppUser::getPasswordHash).orElse(dummyHash));

        if (found.isEmpty() || !passwordOk || !found.get().isActive()) {
            rateLimiter.recordFailure(email, ipAddress);
            String reason = found.isEmpty() ? "UNKNOWN_EMAIL" : (!passwordOk ? "BAD_PASSWORD" : "INACTIVE");
            audit.record(found.map(AppUser::getId).orElse(null), AuditAction.LOGIN_FAILED, "USER",
                    found.map(AppUser::getId).orElse(null), Map.of("email", email, "reason", reason), ipAddress);
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }

        AppUser user = found.get();
        rateLimiter.reset(email, ipAddress);
        user.recordLogin(clock.instant());   // saved automatically at commit (JPA dirty checking)

        JwtService.IssuedToken token = jwtService.issue(user);
        audit.record(user.getId(), AuditAction.LOGIN_SUCCESS, "USER", user.getId(), Map.of(), ipAddress);

        long expiresIn = Duration.between(clock.instant(), token.expiresAt()).toSeconds();
        return new LoginResponse(token.value(), "Bearer", expiresIn, token.expiresAt(), UserMapper.toResponse(user));
    }
}
